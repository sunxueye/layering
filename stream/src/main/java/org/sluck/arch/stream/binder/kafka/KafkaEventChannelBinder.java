package org.sluck.arch.stream.binder.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.sluck.arch.stream.binder.ConsumerAndProducerEventChannels;
import org.sluck.arch.stream.channel.EventChannel;
import org.sluck.arch.stream.channel.EventChannelFactory;
import org.sluck.arch.stream.properties.EventChannelPropertiesGetter;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 事件通道与具体的消息队列绑定对象
 * <p>
 * Created by sunxy on 2019/3/27 20:03.
 */
public class KafkaEventChannelBinder implements EnvironmentAware, ApplicationContextAware {

    //集群配置前缀
    private static final String BROKER_CLUSTER_CONFIG_PREFIX = "application.brokerCluster.kafka.serverAddress.";

    private Environment environment;

    private ApplicationContext applicationContext;

    //key 为 brokerClusterName
    private Map<String, ConsumerAndProducerEventChannels> cacheChannelConfigures = new HashMap<>(); //缓存的 channel 配置信息

    private Map<String, List<KafkaConsumeTaskRunner>> allConsumerTaskRunners = new HashMap<>(); //key 为集群名称， value 为其对应的所有的消费任务执行器

    /**
     * kafka broker client 启动
     *
     * @param channelFactory
     */
    public void brokerClientStart(EventChannelFactory channelFactory) {
        Map<String, List<EventChannel>> clusterAndChannels = channelFactory.getBrokerClusterAndChannels();
        clusterAndChannels.keySet().forEach(cluster -> {
                    cacheChannelConfigures.put(
                            //构造不同的集群配置信息
                            cluster, new ConsumerAndProducerEventChannels(clusterAndChannels.get(cluster))
                    );

                    //开启集群 client
                    doStartByCluster(cluster);
                }
        );
    }

    private void doStartByCluster(String brokerClusterName) {
        String brokerAddress = environment.getProperty(BROKER_CLUSTER_CONFIG_PREFIX + brokerClusterName);
        if (brokerAddress == null || "".equals(brokerAddress)) {
            throw new IllegalArgumentException("broker 集群:" + brokerClusterName + " 对应的地址不存在");
        }

        //为每个集群配置相应的配置获取器
        EventChannelPropertiesGetter propertiesGetter = null;
        String[] getterNames = applicationContext.getBeanNamesForType(EventChannelPropertiesGetter.class);
        if (getterNames.length == 1) {
            propertiesGetter = applicationContext.getBean(EventChannelPropertiesGetter.class);
        } else if (getterNames.length > 1) {
            propertiesGetter = applicationContext.getBean(brokerClusterName, EventChannelPropertiesGetter.class);
        }

        //配置集群下所有消费者 和其 topics 信息
        final EventChannelPropertiesGetter getter = propertiesGetter;
        Map<String, List<String>> consumerAndTopics = cacheChannelConfigures.get(brokerClusterName).getConsumerAndTopics();
        consumerAndTopics.keySet().forEach(consumer -> {
            List<String> topics = consumerAndTopics.get(consumer);
            KafkaConsumeTaskRunner runner = configureKafkaConsumer(getter, brokerClusterName, consumer, brokerAddress, topics);
            List<KafkaConsumeTaskRunner> runners = allConsumerTaskRunners.computeIfAbsent(brokerClusterName, k -> new ArrayList<>());
            runners.add(runner);

            //启动消费Task
            runner.start();
        });

        //配置集群下所有生产者

    }

    private KafkaConsumeTaskRunner configureKafkaConsumer(EventChannelPropertiesGetter propertiesGetter, String brokerClusterName, String consumer,
                                        String brokerAddress, List<String> topics) {
        Map<String, Object> consumerConfg = null;
        if (propertiesGetter != null) {
            consumerConfg = propertiesGetter.getConsumerProps(consumer);
        }
        if (consumerConfg == null) {
            consumerConfg = getDefaultKafkaConsumerProps();
        }

        //配置 broker 地址与 消费者信息
        consumerConfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerAddress);
        consumerConfg.put(ConsumerConfig.GROUP_ID_CONFIG, consumer);
        //key 定死为 string 类型
        consumerConfg.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        //创建 kafka 消费者
        KafkaConsumer<String, Object> kafkaConsumer = new KafkaConsumer<>(consumerConfg);

        //为消费者配置任务执行器
        boolean autoCommit = consumerConfg.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG).toString().equals("true");
        KafkaConsumeTaskRunner runner = new KafkaConsumeTaskRunner(brokerClusterName, consumer, kafkaConsumer,
                cacheChannelConfigures.get(brokerClusterName), autoCommit, 8);
        kafkaConsumer.subscribe(topics, runner);
        return runner;
    }

    private Map<String, Object> getDefaultKafkaConsumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "500");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, "2000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    private Map<String, Object> getDefaultKafkaProducerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 524288);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        return props;
    }

    @PreDestroy
    public void shutdown() {
        //停止所有消费任务
        allConsumerTaskRunners.values().forEach(list -> list.forEach(KafkaConsumeTaskRunner::shutdown));

        //关闭总的线程池
        ExecutorServiceFactory.shutdown();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
