package org.sluck.arch.soawar.client;

import com.netflix.hystrix.contrib.javanica.annotation.DefaultProperties;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

/**
 * 类级别 断路器默认配置
 *
 * Created by sunxy on 2019/3/7 10:49.
 */
@DefaultProperties(
        commandProperties = {
                @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000"),
                @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "5"),
                @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
                @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "5"),
                @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "10000"),
                @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "5")
        },
        threadPoolProperties = {
                @HystrixProperty(name = "coreSize", value = "5"),
                @HystrixProperty(name = "maxQueueSize", value = "100")
        })
public class HystrixDefaultConfigure {
}
