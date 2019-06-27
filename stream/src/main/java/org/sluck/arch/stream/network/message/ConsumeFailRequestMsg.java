package org.sluck.arch.stream.network.message;

/**
 * 消费失败请求报文
 *
 * author: sunxy
 * createTime: 2019/6/20:15:49
 * since: 1.0.0
 */
public class ConsumeFailRequestMsg implements RequestMessage{

    private String groupName;

    private String topic;

    private String eventJson; //事件 json

    public ConsumeFailRequestMsg() {
    }

    public ConsumeFailRequestMsg(String groupName, String topic, String eventJson) {
        this.groupName = groupName;
        this.topic = topic;
        this.eventJson = eventJson;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getEventJson() {
        return eventJson;
    }

    public void setEventJson(String eventJson) {
        this.eventJson = eventJson;
    }
}
