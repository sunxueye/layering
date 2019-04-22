package org.sluck.arch.stream.annotation;

import org.sluck.arch.stream.config.EventChannelBindingConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 允许使用事件处理器机制
 *
 * Created by sunxy on 2019/3/27 18:10.
 */
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@Import({ EventChannelBindingConfiguration.class})
public @interface EnableEventHandler {
}
