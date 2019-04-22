package org.sluck.arch.stream.annotation;

import org.sluck.arch.stream.converter.EventConverter;

import java.lang.annotation.*;

/**
 * 事件监听器注解，所修饰的方法由于 事件转换器（{@link EventConverter} 目前只对一个参数进行转换，
 * 所以该注解修饰的方法参数不能超过一个
 *
 * Created by sunxy on 2019/3/27 9:45.
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

    /**
     * 订阅的 topic 名称
     *
     * @return
     */
    String topic();

    /**
     * 设置消费者名称，如果不设置则使用系统默认的消费者属性
     * （默认的消费者名称由配置文件定义 application.default.consumer=xxx）
     *
     * @return
     */
    String consumer() default "";

    /**
     * 设置所属的 broker 集群，当应用中同时使用了多个 broker 集群时候使用，否则使用默认的 borker 集群名称
     * （默认的集群名称由配置文件定义 application.default.brokerCluster=xxx)
     *
     * @return
     */
    String brokerCluster() default "";
}
