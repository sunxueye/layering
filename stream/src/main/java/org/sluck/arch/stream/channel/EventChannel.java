package org.sluck.arch.stream.channel;

import java.util.Objects;

/**
 * 事件通道基类，由 broker 集群名称， topic 和 group 三者唯一确定
 *
 * Created by sunxy on 2019/3/27 15:02.
 */
public abstract class EventChannel {

    private final String brokerClusterName;

    private final String topicName;

    private final String groupName;

    /**
     * 使用 broker 集群名称， topic 名称， group 名称唯一确定通道
     *
     * @param brokerClusterName
     * @param topicName
     * @param groupName
     */
    public EventChannel(String brokerClusterName, String topicName, String groupName) {
        this.brokerClusterName = brokerClusterName;
        this.topicName = topicName;
        this.groupName = groupName;
    }

    public String getBrokerClusterName() {
        return brokerClusterName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventChannel)) return false;
        EventChannel that = (EventChannel) o;
        return brokerClusterName.equals(that.brokerClusterName) &&
                topicName.equals(that.topicName) &&
                groupName.equals(that.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brokerClusterName, topicName, groupName);
    }
}
