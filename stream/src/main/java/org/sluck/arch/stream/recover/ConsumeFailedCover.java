package org.sluck.arch.stream.recover;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.channel.ConsumerEventChannel;
import org.sluck.arch.stream.coordinator.MqCoordinator;
import org.sluck.arch.stream.exception.MessageSendException;
import org.sluck.arch.stream.network.message.ConsumeFailRequestMsg;
import org.sluck.arch.stream.network.message.ConsumeFailResponseMsg;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.sluck.arch.stream.util.thread.PoolThreadFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 消费失败恢复对象
 * <p>
 * author: sunxy
 * createTime: 2019/6/19:14:22
 * since: 1.0.0
 */
public class ConsumeFailedCover {

    private static final String CONSUME_FAIL_PERISIT = "consume.fail.persist"; //失败持久化功能

    private static final String CONSUME_FAIL_RETRY_COUNT = "consume.fail.retry.count"; //消费失败重试次数

    @Value("${consume.fail.persist}")
    private boolean failPersis;

    @Value("${consume.fail.retry.count:15}")
    private int retryCount;

    @Resource
    private MqCoordinator coordinator;

    private ScheduledExecutorService scheduledService = ExecutorServiceFactory.newScheduledThreadPool(5, new PoolThreadFactory("retry_failed_msg_pool", true));

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void handleFailedMessage(ConsumerEventChannel eventChannel, Object event) {

        if (failPersis) {
            int tryCount = 3; //默认重试次数
            try {
                String eventJson = JSON.toJSONString(event);
                ConsumeFailRequestMsg requestMsg = new ConsumeFailRequestMsg(eventChannel.getGroupName(), eventChannel.getTopicName(), eventJson);
                ConsumeFailResponseMsg responseMsg = saveToCoordinator(requestMsg, tryCount);
                //为空说明消息发送失败
                if (responseMsg != null && responseMsg.isPersistSuccess()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("失败消息保存成功:{}", eventJson);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("发送失败消息系统异常", e);
            }
        }
        //否则进行本地重试
        retryLocal(eventChannel, event);
    }

    /**
     * 保存至远程 mq 协调者
     *
     * @param msg
     * @param tryCount 保存失败的情况下 重试的次数
     * @return 请求失败返回 null
     */
    private ConsumeFailResponseMsg saveToCoordinator(ConsumeFailRequestMsg msg, int tryCount) {
        if (tryCount < 1) {
            return null;
        }
        try {
            return coordinator.sendMessageSyn(msg, 2, TimeUnit.SECONDS);//2 秒超时
        } catch (TimeoutException e) {
            logger.warn("发送消费失败消息至 mq 协调者超时, 取消发送");
        } catch (MessageSendException e) {
            logger.warn("发送消费失败消息至 mq 协调者失败, 准备重试:{}, 异常信息:{}", tryCount, e.getMessage());
            return saveToCoordinator(msg, tryCount - 1);
        }

        return null;
    }

    /**
     * 本地尝试重试
     */
    public void retryLocal(ConsumerEventChannel eventChannel, Object event) {
        //重试次数大于 0 的情况下再重试
        if (retryCount > 0) {
            LocalRetryFailedMsgTask task = new LocalRetryFailedMsgTask(retryCount, eventChannel, event, scheduledService);
            scheduledService.schedule(task, 2, TimeUnit.SECONDS);//延迟两秒后执行
        }
    }

    /**
     * 本地重试消费失败消息任务
     */
    private static class LocalRetryFailedMsgTask implements Runnable {

        private int totalCount;//总的重试次数

        private int retryCount = 0; //已经重试次数

        private ConsumerEventChannel eventChannel;

        private Object event;

        private ScheduledExecutorService scheduledService;

        public LocalRetryFailedMsgTask(int totalCount, ConsumerEventChannel eventChannel, Object event, ScheduledExecutorService scheduledService) {
            this.totalCount = totalCount;
            this.eventChannel = eventChannel;
            this.event = event;
            this.scheduledService = scheduledService;
        }

        @Override
        public void run() {
            if (retryCount > totalCount) {
                return;
            }

            boolean success = eventChannel.consumeEvent(event);
            if (!success) {
                //不是成功的话计算下次通知时间
                retryCount ++;
                long delaySecond = 1L << retryCount;
                scheduledService.schedule(this, delaySecond, TimeUnit.SECONDS);
            }
        }
    }

}
