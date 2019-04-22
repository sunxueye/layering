package org.sluck.arch.stream.binder.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.binder.ConsumerAndProducerEventChannels;
import org.sluck.arch.stream.channel.ConsumerEventChannel;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.sluck.arch.stream.util.thread.PoolThreadFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
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

    private String brokerCluster;
    private String consumerName;

    private KafkaConsumer<String, Object> consumer;

    private ConsumerAndProducerEventChannels eventChannels; //所在集群下的所有消费者与生产者事件通道

    private ConcurrentHashMap<TopicPartition, OffsetCommitQueue> topicAndUncommitOffsets = new ConcurrentHashMap<>();
    private Set<TopicPartition> needCleanList = new HashSet<>();

    private ExecutorService execThreadPool;

    private ExecutorService pollThread;

    private ScheduledExecutorService commitThread; //提交 offset 线程

    private boolean autoCommit;

    private volatile boolean shutdown;

    private AtomicInteger unConsumeRecordCount = new AtomicInteger(0); //未消费的任务数量

    private Logger logger = LoggerFactory.getLogger(getClass());

    public KafkaConsumeTaskRunner(String brokerCluster, String consumerName, KafkaConsumer<String, Object> consumer,
                                  ConsumerAndProducerEventChannels eventChannels, boolean autoCommit, int taskSize) {
        this.brokerCluster = brokerCluster;
        this.consumerName = consumerName;

        this.consumer = consumer;
        this.eventChannels = eventChannels;

        this.autoCommit = autoCommit;

        //初始化 pollThread
        pollThread = ExecutorServiceFactory.newSingleThreadExecutor(new PoolThreadFactory(consumerName + "_pollThread"));

        //初始化提交 offsetThread
        commitThread = ExecutorServiceFactory.newScheduledThreadPool(1, new PoolThreadFactory(consumerName + "_commitThread"));

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
                commitOffsetAsync();
                poll();
            }
        });

        //开始 commit_offset 线程任务
        commitThread.scheduleAtFixedRate(() -> {
            commitOffsetSync();
            cleanOffsetQueue();
        }, 3, 1, TimeUnit.SECONDS); //1s 提交 1次
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
        ConsumerRecords<String, Object> records = consumer.poll(Duration.ofSeconds(5));
        unConsumeRecordCount.addAndGet(records.count());
        for (TopicPartition partition : records.partitions()) {
            List<ConsumerRecord<String, Object>> partitionRecords = records.records(partition);
            for (ConsumerRecord<String, Object> record : partitionRecords) {
                Runnable task = makeConsumeTask(record);
                if (autoCommit) {
                    //自动提交的话自己执行
                    task.run();
                } else {
                    execThreadPool.submit(task);
                }
            }
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
                        logger.debug("_offset 异步提交成功");
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
            if (queue.isNeedCommit()) {
                Long offset = queue.poll();
                if (offset != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("topic:" + tp.topic() + ", part:" + tp.partition() + ", offset:" + offset);
                    }
                    offsets.put(tp, new OffsetAndMetadata(offset));
                }
            }
        });
        return offsets;
    }


    /**
     * 清理需要清理的 offset 队列
     */
    private void cleanOffsetQueue() {
        List<TopicPartition> needList = new ArrayList<>();
        needCleanList.forEach(tp -> {
            OffsetCommitQueue queue = topicAndUncommitOffsets.get(tp);
            if (!queue.isNeedCommit()) {
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
            logger.info("接受到消费事件消息【" + brokerCluster + "】,【" + consumerName + "】【key】 :" + record.key());
            boolean success = eventChannel.consumeEvent(record.value());
            logger.info("消息处理完毕，处理结果:" + success);

            //更新未消费的 record 数量
            unConsumeRecordCount.decrementAndGet();
            if (success) {
                if (logger.isDebugEnabled()) {
                    logger.debug("更新消息消费进度, topic: " + record.topic() + ", part:" + record.partition() + ", offset :" + record.offset());
                }
                OffsetCommitQueue offsetQueue = topicAndUncommitOffsets.computeIfAbsent(
                        new TopicPartition(record.topic(), record.partition()), k -> new OffsetCommitQueue());
                //提交的为 下一次需要拉取的 offset 位置
                offsetQueue.add(record.offset() + 1);
            } else {
                //消费失败的情况处理
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
            partitions.forEach(tp -> stringBuilder.append(tp.toString()));
            logger.debug("消费者组重新分区完毕, 此消费者新的分区信息:" + stringBuilder.toString());
        }

        //清空所有待清空 list
        needCleanList.clear();

        //清理已经不属于此消费者的 queue
        List<TopicPartition> needList = new ArrayList<>();
        Set<TopicPartition> laterCleanList = new HashSet<>();
        topicAndUncommitOffsets.keySet().stream()
                .filter(tp -> !partitions.contains(tp))
                .forEach(tp -> {
                    OffsetCommitQueue queue = topicAndUncommitOffsets.get(tp);
                    if (!queue.isNeedCommit()) {
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
