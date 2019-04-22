package org.sluck.arch.stream.properties;

import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;

/**
 * 事件相关的生产者 消费者 配置属性获取器
 * 使用者需要实现此接口，event 处理机制根据此接口来获取配置信息.
 *
 * 实现类需要将自己注入到 spring bean factory 中，且有多个实现的情况下使用
 * ({@link Qualifier} 注解标注对应的 broker cluster 名称，自动配置的时候会根据集群名称获取对应的获取器
 *
 * Created by sunxy on 2019/3/27 21:13.
 */
public interface EventChannelPropertiesGetter {

    /**
     * 根据消费者名称获取其对应的配置信息
     *
     * @param consumerName
     * @return
     */
    Map<String, Object> getConsumerProps(String consumerName);

    /**
     * 根据生产者名称获取其对应的配置信息
     *
     * @param producerName
     * @return
     */
    Map<String, Object> getProducerProps(String producerName);
}
