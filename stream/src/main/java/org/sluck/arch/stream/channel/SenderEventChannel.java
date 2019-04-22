package org.sluck.arch.stream.channel;

/**
 * 发送类型的事件通道，内部包含了事件发送器
 *
 * Created by sunxy on 2019/3/27 15:13.
 */
public class SenderEventChannel extends EventChannel {

    /**
     * 使用 broker 集群名称， topic 名称， group 名称唯一确定通道
     *
     * @param brokerClusterName
     * @param topicName
     * @param groupName
     */
    public SenderEventChannel(String brokerClusterName, String topicName, String groupName) {
        super(brokerClusterName, topicName, groupName);
    }
}
