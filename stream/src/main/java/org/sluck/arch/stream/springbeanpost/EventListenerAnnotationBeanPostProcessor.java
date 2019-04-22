package org.sluck.arch.stream.springbeanpost;

import org.sluck.arch.stream.annotation.EventListener;
import org.sluck.arch.stream.channel.EventChannelFactory;
import org.sluck.arch.stream.converter.EventConverter;
import org.sluck.arch.stream.invokehander.EventListenerMethodHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * 基于 spring bean post processor 的 事件监听器注解处理器
 * {@link BeanPostProcessor}
 * {@link EventListener}
 *
 * Created by sunxy on 2019/3/27 10:25.
 */
public class EventListenerAnnotationBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, SmartInitializingSingleton {

    private ConfigurableApplicationContext applicationContext;

    private final Set<Runnable> eventListenerCallbacks = new HashSet<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.isAopProxy(bean) ? AopUtils.getTargetClass(bean)
                : bean.getClass();
        Method[] uniqueDeclaredMethods = ReflectionUtils.getUniqueDeclaredMethods(targetClass);
        for (Method method : uniqueDeclaredMethods) {
            EventListener eventListener = AnnotatedElementUtils.findMergedAnnotation(method, EventListener.class);
            if (eventListener != null && !method.isBridge()) {
                this.eventListenerCallbacks.add(() -> {
                    this.doPostProcess(eventListener, method, bean);
                });
            }
        }
        return bean;
    }

    private void doPostProcess(EventListener eventListener, Method method, Object bean) {
        //获取事件转换器，这里是唯一的，如果有需要以后需要改成多个事件转换器
        EventConverter eventConverter = applicationContext.getBean(EventConverter.class);
        EventListenerMethodHandler methodHandler = new EventListenerMethodHandler(bean, method, eventConverter);

        //准备注册通道
        EventChannelFactory eventChannelFactory = applicationContext.getBean(EventChannelFactory.class);
        String brokerCluster = eventListener.brokerCluster();
        String topicName = eventListener.topic();
        String consumerName = eventListener.consumer();
        if (brokerCluster.equals("") && consumerName.equals("")) {
            eventChannelFactory.addConsumerEventChannel(topicName, methodHandler);
        } else if (!brokerCluster.equals("") && !consumerName.equals("")) {
            eventChannelFactory.addConsumerEventChannel(brokerCluster, topicName, consumerName, methodHandler);
        } else if (brokerCluster.equals("")) {
            eventChannelFactory.addConsumerEventChannel(topicName, consumerName, methodHandler);
        } else {
            throw new IllegalArgumentException("不能出现默认的消费者名称为空, broker 集群不为空");
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.eventListenerCallbacks.forEach(Runnable::run);
    }
}
