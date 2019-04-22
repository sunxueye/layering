package org.sluck.arch.stream.channel;

import org.sluck.arch.stream.invokehander.EventListenerMethodHandler;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件通道对象工厂
 *
 * Created by sunxy on 2019/3/27 17:04.
 */
public class EventChannelFactory implements EnvironmentAware {

    private static final String DEFALUT_CONSUMER_PREFIX = "application.default.consumer";
    private static final String DEFALUT_BROKER_CLUSTER_PREFIX = "application.default.brokerCluster";
    private static final String DEFALUT_PRODUCER_PREFIX = "application.default.producer";

    private String defalutConsumer;

    private String defaultBrokerCluster;

    private String defalutProducer;

    private Environment environment;

    private final ConcurrentHashMap<EventChannel, Object> cachedChannels = new ConcurrentHashMap<>();

    /**
     * 增加消费事件通道
     *
     * @param brokerClusterName
     * @param topicName
     * @param groupName
     * @param eventHandler
     */
    public void addConsumerEventChannel(String brokerClusterName, String topicName, String groupName,
                                        EventListenerMethodHandler eventHandler) {
        ConsumerEventChannel channel = new ConsumerEventChannel(brokerClusterName, topicName, groupName, eventHandler);

        cachedChannels.putIfAbsent(channel, new Object());
    }

    /**
     * 增加消费事件通道，使用默认的集群名称
     *
     * @param topicName
     * @param groupName
     * @param eventHandler
     */
    public void addConsumerEventChannel(String topicName, String groupName,
                                        EventListenerMethodHandler eventHandler) {
        if (defaultBrokerCluster == null) {
            throw new IllegalArgumentException("默认的集群名称为空, 不能使用该方法");
        }
        ConsumerEventChannel channel = new ConsumerEventChannel(defaultBrokerCluster, topicName, groupName, eventHandler);

        cachedChannels.putIfAbsent(channel, new Object());
    }

    /**
     * 增加消费事件通道，使用默认的集群名称 和 消费者名称
     *
     * @param topicName
     * @param eventHandler
     */
    public void addConsumerEventChannel(String topicName, EventListenerMethodHandler eventHandler) {
        if (defaultBrokerCluster == null || defalutConsumer == null) {
            throw new IllegalArgumentException("默认的集群名称或默认的消费者名称为空, 不能使用该方法");
        }
        ConsumerEventChannel channel = new ConsumerEventChannel(defaultBrokerCluster, topicName, defalutConsumer, eventHandler);

        cachedChannels.putIfAbsent(channel, new Object());
    }

    /**
     * 获取所有的 broker 集群名称，和其对应的所有的事件通道
     *
     * @return
     */
    public Map<String, List<EventChannel>> getBrokerClusterAndChannels() {
        Map<String, List<EventChannel>> res = new HashMap<>();
        cachedChannels.keySet().forEach(c -> {
            String clusterName = c.getBrokerClusterName();
            List<EventChannel> channels = res.computeIfAbsent(clusterName, k -> new ArrayList<>());
            channels.add(c);
        });

        return res;
    }

    @PostConstruct
    private void initDefaultChannelInfo() {
        this.defalutConsumer = environment.getProperty(DEFALUT_CONSUMER_PREFIX);
        this.defaultBrokerCluster = environment.getProperty(DEFALUT_BROKER_CLUSTER_PREFIX);
        this.defalutProducer = environment.getProperty(DEFALUT_PRODUCER_PREFIX);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
