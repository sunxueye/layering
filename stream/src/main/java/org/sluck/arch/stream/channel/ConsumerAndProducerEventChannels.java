package org.sluck.arch.stream.channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存了同一 broker 集群下 所有消费者和生产者对应的 eventChannel 信息
 *
 * Created by sunxy on 2019/3/28 10:53.
 */
public class ConsumerAndProducerEventChannels {

    private Map<String, List<ConsumerEventChannel>> consumers = new HashMap<>();
    private Map<String, List<SenderEventChannel>> producers = new HashMap<>();

    private Map<String, List<String>> consumerAndTopics = new HashMap<>();
    private Map<String, List<String>> producerAndTopics = new HashMap<>();

    //key = consumer + "-" + topic
    private Map<String, ConsumerEventChannel> consumerTopicAndChannels = new HashMap<>();
    private Map<String, SenderEventChannel> producerTopicAndChannels = new HashMap<>();

    /**
     * 根据所有 channel 划分为生产者和消费者 event channel
     *
     * @param channels
     */
    public ConsumerAndProducerEventChannels(List<EventChannel> channels) {
        channels.forEach(c -> {
            String group = c.getGroupName();
            if (c instanceof ConsumerEventChannel) {
                List<ConsumerEventChannel> consumerEventChannels = consumers.computeIfAbsent(group, k -> new ArrayList<>());
                consumerEventChannels.add((ConsumerEventChannel) c);

                List<String> topics = consumerAndTopics.computeIfAbsent(group, k -> new ArrayList<>());
                topics.add(c.getTopicName());

                String consumerAndTopic = makeGroupAndTopicKey(group, c.getTopicName());
                consumerTopicAndChannels.put(consumerAndTopic, (ConsumerEventChannel) c);
            } else if (c instanceof SenderEventChannel) {
                List<SenderEventChannel> senderEventChannels = producers.computeIfAbsent(group, k -> new ArrayList<>());
                senderEventChannels.add((SenderEventChannel) c);

                List<String> topics = producerAndTopics.computeIfAbsent(group, k -> new ArrayList<>());
                topics.add(c.getTopicName());

                String producerAndTopic = makeGroupAndTopicKey(group, c.getTopicName());
                producerTopicAndChannels.put(producerAndTopic, (SenderEventChannel) c);
            }
        });
    }

    private String makeGroupAndTopicKey(String group, String topic) {
        return group + "-" + topic;
    }

    /**
     * 根据消费者名称和其订阅的 topic 获取对应的事件通道
     *
     * @param consumer
     * @param topic
     * @return
     */
    public ConsumerEventChannel getByConsumerAndTopic(String consumer, String topic) {
        return consumerTopicAndChannels.get(makeGroupAndTopicKey(consumer, topic));
    }

    /**
     * 根据生产者名称和其订阅的 topic 获取对应的事件通道
     *
     * @param producer
     * @param topic
     * @return
     */
    public SenderEventChannel getByProducerAndTopic(String producer, String topic) {
        return producerTopicAndChannels.get(makeGroupAndTopicKey(producer, topic));
    }

    public Map<String, List<String>> getConsumerAndTopics() {
        return consumerAndTopics;
    }
}
