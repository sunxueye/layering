package org.sluck.arch.stream.binder.kafka;

import org.apache.kafka.clients.KafkaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * kafka - spring 整合 配置
 *
 * author: sunxy
 * createTime: 2019/6/3:16:43
 * since: 1.0.0
 */
@Configuration
@ConditionalOnClass(KafkaClient.class)
public class KafkaSpringConfig {

    @Bean
    public KafkaEventChannelBinder kafkaEventChannelBinder() {
        return new KafkaEventChannelBinder();
    }
}
