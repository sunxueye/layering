package org.sluck.arch.stream.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.invokehander.EventListenerMethodHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 消费类型的通道，内部包含对应的事件处理器
 * <p>
 * Created by sunxy on 2019/3/27 15:10.
 */
public class ConsumerEventChannel extends EventChannel {

    private final List<EventListenerMethodHandler> eventHandlers = new ArrayList<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 使用 broker 集群名称， topic 名称， group 名称唯一确定通道
     *
     * @param brokerClusterName
     * @param topicName
     * @param groupName
     */
    public ConsumerEventChannel(String brokerClusterName, String topicName, String groupName,
                                EventListenerMethodHandler eventHandler) {
        super(brokerClusterName, topicName, groupName);
        addHandler(eventHandler);
    }

    public void addHandler(EventListenerMethodHandler handler) {
        this.eventHandlers.add(handler);
    }

    /**
     * 消费事件
     *
     * @param parms 事件处理方法需要的参数
     * @return 是否消费成功
     */
    public boolean consumeEvent(Object... parms) {
        boolean success = true;
        for (EventListenerMethodHandler handler : eventHandlers) {
            try {
                Object res = handler.invoke(parms);
                if (res instanceof Boolean) {
                     if(!(boolean) res) {
                         //有一个失败则为失败
                         success = false;
                     }
                }
                logger.info("{} 执行完毕, 执行结果: {}", handler.getMethodName(), res);
            } catch (Exception | Error e) {
                logger.error("{} 事件消费处理失败", handler.getMethodName(), e);
                success = false;
            }
        }
        //默认没有异常就是消费成功
        return success;
    }
}
