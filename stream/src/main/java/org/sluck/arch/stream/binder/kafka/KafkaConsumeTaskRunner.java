package org.sluck.arch.stream.binder.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.channel.ConsumerAndProducerEventChannels;
import org.sluck.arch.stream.channel.ConsumerEventChannel;
import org.sluck.arch.stream.recover.ConsumeFailedCover;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.sluck.arch.stream.util.thread.PoolThreadFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * kafka 消费者任务执行器，内部封装了对 consumer 执行的 poll 操作
 * 如果手动提交 offset，则不会使用线程池
 * 线程池只有在不自动提交 offset 的情况下，才会使用
 * <p>
 * Created by sunxy on 2019/3/28 11:04.
 */
public class KafkaConsumeTaskRunner implements ConsumerRebalanceListener {

    private static final int MAX_UN_CONSUME_RECORD_COUNT = 1000; //最多积攒 x 个未消费的消息，超过此阈值 不在 poll 消息
    private static final Long MAX_WAIT_MS = 3000L; //主线程最多睡眠时间 ms
    private static final int MAX_POLL_WAIT_SE = 5; //poll 操作最多等待 秒数

    private String brokerCluster;
    private String consumerName;

    private KafkaConsumer<String, Object> consumer;

    private ConsumerAndProducerEventChannels eventChannels; //所在集群下的所有消费者与生产者事件通道

    private ConcurrentHashMap<TopicPartition, OffsetCommitQueue> topicAndUncommitOffsets = new ConcurrentHashMap<>();
    private ConcurrentHashMap<TopicPartition, Long> lastCommitOffsets = new ConcurrentHashMap<>();//上次提交的 offset
    private Set<TopicPartition> needCleanList = new HashSet<>();

    private ExecutorService execThreadPool;

    private ExecutorService pollThread;

    private boolean autoCommit;

    private volatile boolean shutdown;

    private AtomicInteger unConsumeRecordCount = new AtomicInteger(0); //未消费的任务数量

    private ConsumeFailedCover failedCover; //消费失败处理器

    private Logger logger = LoggerFactory.getLogger(getClass());

    public KafkaConsumeTaskRunner(String brokerCluster, String consumerName, KafkaConsumer<String, Object> consumer,
                                  ConsumerAndProducerEventChannels eventChannels, boolean autoCommit, int taskSize, ConsumeFailedCover failedCover) {
        this.brokerCluster = brokerCluster;
        this.consumerName = consumerName;

        this.consumer = consumer;
        this.eventChannels = eventChannels;

        this.autoCommit = autoCommit;

        this.failedCover = failedCover;

        //初始化 pollThread
        pollThread = ExecutorServiceFactory.newSingleThreadExecutor(new PoolThreadFactory(consumerName + "_pollThread"));

        //初始化线程池
        initThreadPool(consumerName, autoCommit, taskSize);
    }

    /**
     * 启动消费任务
     */
    public void start() {
        //开始执行任务线程池任务
        pollThread.submit(() -> {
            while (!shutdown) {
                if (!autoCommit) {
                    commitOffsetSync();
                    cleanOffsetQueue();
                }
                poll();
            }
        });
    }

    /**
     * 执行 poll， 从 kafkaConsumer poll 消息并根据情况分配任务执行
     */
    private void poll() {
        if (unConsumeRecordCount.get() >= MAX_UN_CONSUME_RECORD_COUNT) {
            logger.info("集群:" + brokerCluster + ",消费者:" + consumerName + ", 未消费消息已经达到最大阈值:" + unConsumeRecordCount.get());
            try {
                TimeUnit.MILLISECONDS.sleep(MAX_WAIT_MS);
            } catch (InterruptedException e) {
                logger.error("thread sleep interrupted", e);
            }
            return;
        }

        //执行 poll 操作
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(MAX_POLL_WAIT_SE));
        unConsumeRecordCount.addAndGet(records.count());
        for (TopicPartition partition : records.partitions()) {
            List<ConsumerRecord<String, Object>> partitionRecords = records.records(partition);
            //构造 part 对应的 保存还未消费的 offset 队列
            OffsetCommitQueue offsetQueue = topicAndUncommitOffsets.computeIfAbsent(partition, k -> new OffsetCommitQueue());
            List<Long> offsets = new ArrayList<>();
            List<Runnable> tasks = new ArrayList<>();
            for (ConsumerRecord<String, Object> record : partitionRecords) {
                //添加还未消费的 offset
                offsets.add(record.offset());
                tasks.add(makeConsumeTask(record));
            }
            offsetQueue.addOffset(offsets);
            tasks.forEach(task -> {
                if (autoCommit) {
                    //自动提交的话自己执行
                    task.run();
                } else {
                    execThreadPool.submit(task);
                }
            });
        }
    }

    /**
     * 同步执行提交 offset
     */
    private void commitOffsetSync() {

        Map<TopicPartition, OffsetAndMetadata> offsets = prepareCommitOffset();

        if (!offsets.isEmpty()) {
            try {
                consumer.commitSync(offsets);
                if (logger.isDebugEnabled()) {
                    logger.debug("_offset 同步提交成功");
                }
                //更新上次提交记录
                offsets.keySet().forEach(tp -> lastCommitOffsets.put(tp, offsets.get(tp).offset()));
            } catch (Exception | Error e) {
                logger.error("commit_offsets is exception", e);
            }
        }
    }

    /**
     * 异步提交 offset
     */
    private void commitOffsetAsync() {

        Map<TopicPartition, OffsetAndMetadata> offsets = prepareCommitOffset();

        if (!offsets.isEmpty()) {
            try {
                consumer.commitAsync(offsets, (tp, offset) -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("_offset 异步提交申请成功");
                    }
                });
            } catch (Exception | Error e) {
                logger.error("commit_offsets is exception", e);
            }
        }
    }

    /**
     * 准备需要提交的 topic 与 对应的 offset
     */
    private Map<TopicPartition, OffsetAndMetadata> prepareCommitOffset() {
        if (logger.isDebugEnabled()) {
            logger.debug("准备开始 commit_offset 任务");
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        topicAndUncommitOffsets.keySet().forEach(tp -> {
            OffsetCommitQueue queue = topicAndUncommitOffsets.get(tp);
            Long offset = queue.getNeedCommitOffset();
            if (offset != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("topic:" + tp.topic() + ", part:" + tp.partition() + ", offset:" + offset);
                }
                offsets.put(tp, new OffsetAndMetadata(offset + 1)); //下一个需要被消费的 msgId
            }
        });
        return filterNeedCommit(offsets);
    }

    /**
     * 过滤并只保留需要提交的 offset
     *
     * @param offsets
     * @return
     */
    private Map<TopicPartition, OffsetAndMetadata> filterNeedCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        //如果需要提交的 offset <= 上次提交的 offset ，则不需要再次提交
        Map<TopicPartition, OffsetAndMetadata> res = new HashMap<>();
        offsets.keySet().forEach(tp -> {
            Long lastCommit = lastCommitOffsets.get(tp);
            if (lastCommit == null || lastCommit < offsets.get(tp).offset()) {
                res.put(tp, offsets.get(tp));
            }
        });
        return res;
    }


    /**
     * 清理需要清理的 offset 队列
     */
    private void cleanOffsetQueue() {
        List<TopicPartition> needList = new ArrayList<>();
        needCleanList.forEach(tp -> {
            OffsetCommitQueue queue = topicAndUncommitOffsets.get(tp);
            if (!queue.hasNoCommitOffset()) {
                topicAndUncommitOffsets.remove(tp);
                needList.add(tp);
            }
        });

        needList.forEach(tp -> needCleanList.remove(tp));
    }

    /**
     * 构造消费任务
     *
     * @param record
     * @return
     */
    private Runnable makeConsumeTask(ConsumerRecord<String, Object> record) {
        ConsumerEventChannel eventChannel = eventChannels.getByConsumerAndTopic(consumerName, record.topic());
        return () -> {
            logger.info("接受到消费事件消息【{}】,【{}】,【key】:{}", brokerCluster, consumerName, record.key());
            boolean success = eventChannel.consumeEvent(record.value());
            logger.info("消息处理完毕，处理结果{}:", success);

            //更新未消费的 record 数量
            unConsumeRecordCount.decrementAndGet();
            if (success) {
                if (logger.isDebugEnabled()) {
                    logger.debug("消息消费成功，topic:{}, part:{}, offset:{} ", record.topic(), record.partition(), record.offset());
                }
            } else {
                //消费失败的情况处理
                logger.info("消息消费失败, topic:{}, part:{}, offset:{} ", record.topic(), record.partition(), record.offset());
                logger.info("消息交由 失败处理器 处理");
                failedCover.handleFailedMessage(eventChannel, record.value());
            }
            //更新消费进度
            OffsetCommitQueue offsetQueue = topicAndUncommitOffsets.computeIfAbsent(
                    new TopicPartition(record.topic(), record.partition()), k -> new OffsetCommitQueue());
            //移除已经消费的 offset
            offsetQueue.removeOffset(record.offset());
            if (logger.isDebugEnabled()) {
                logger.debug("更新消息消费进度, topic:{}, part:{}, offset:{} ", record.topic(), record.partition(), record.offset());
            }
        };
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void initThreadPool(String consumerName, boolean autoCommit, int taskSize) {
        //自动提交由单线程执行
        if (!autoCommit && taskSize > 0) {
            execThreadPool = ExecutorServiceFactory.newFixedThreadPool(taskSize, new PoolThreadFactory(consumerName + "_consumeThread"));
        }
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (logger.isDebugEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            partitions.forEach(tp -> stringBuilder.append(tp.toString()));
            logger.debug("消费者组开始重新分区, 此消费者原始分区信息:" + stringBuilder.toString());
        }

        //尝试立即提交 offset
        commitOffsetSync();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (logger.isDebugEnabled()) {
            StringBuilder stringBuilder = new StringBuilder();
            partitions.forEach(tp -> stringBuilder.append(tp.toString() + ","));
            logger.debug("消费者组重新分区完毕, 此消费者新的分区信息:" + stringBuilder.toString());
        }

        //清理已经不属于此消费者的 queue
        List<TopicPartition> needList = new ArrayList<>();
        Set<TopicPartition> laterCleanList = new HashSet<>();
        topicAndUncommitOffsets.keySet().stream()
                .filter(tp -> !partitions.contains(tp))
                .forEach(tp -> {
                    OffsetCommitQueue queue = topicAndUncommitOffsets.get(tp);
                    if (!queue.hasNoCommitOffset()) {
                        needList.add(tp);
                    } else {
                        laterCleanList.add(tp);
                    }
                });
        //已经没用需要提交的立即清理
        if (!needList.isEmpty()) {
            needList.forEach(tp -> topicAndUncommitOffsets.remove(tp));
        }
        //由 定期线程 稍后清理
        if (!laterCleanList.isEmpty()) {
            needCleanList.addAll(laterCleanList);
        }
    }
}
