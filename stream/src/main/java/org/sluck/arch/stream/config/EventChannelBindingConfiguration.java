package org.sluck.arch.stream.config;

import org.sluck.arch.stream.channel.EventChannelFactory;
import org.sluck.arch.stream.converter.EventConverter;
import org.sluck.arch.stream.converter.FastJsonEventConverter;
import org.sluck.arch.stream.springbeanpost.EventListenerAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

/**
 * 事件通道相关绑定设置
 *
 * Created by sunxy on 2019/3/27 18:09.
 */
@Configurable
public class EventChannelBindingConfiguration {

    @Bean
    private EventListenerAnnotationBeanPostProcessor eventListenerBeanPostProcessor() {
        return new EventListenerAnnotationBeanPostProcessor();
    }

    @Bean
    private EventChannelFactory eventChannelFactory() {
        return new EventChannelFactory();
    }

    @Bean
    private EventConverter fastJsonEventConverter() {
        return new FastJsonEventConverter();
    }

}
