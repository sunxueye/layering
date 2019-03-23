package org.sluck.arch.soawar.client2;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.hystrix.contrib.javanica.cache.annotation.CacheResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.api.TestValues;
import org.sluck.arch.soawar.ribbon.TestRibbonConfigure;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableCircuitBreaker
@EnableHystrixDashboard
@RibbonClient(name = "TestService", configuration = {TestRibbonConfigure.class})
public class ClientApplication2 {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/hello")
    @HystrixCommand(
            //fallbackMethod = "fallTest",
            groupKey = "ClientTestCommandGroup", commandKey = "ClientTestCommand",
            commandProperties = {
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "2000"),
                    @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "5"),
                    @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "5000"),
                    @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "60"),
                    @HystrixProperty(name = "metrics.rollingStats.timeInMilliseconds", value = "10000"),
                    @HystrixProperty(name = "metrics.rollingStats.numBuckets", value = "5")
            },
            threadPoolProperties = {
                    @HystrixProperty(name = "coreSize", value = "15"),
                    @HystrixProperty(name = "maxQueueSize", value = "100")
            }
    )
    //@CacheResult(cacheKeyMethod = "cacheName")
    @ResponseBody
    public String serviceUrl(String name) {
        //List<ServiceInstance> list = discoveryClient.getInstances("TestService");
        //list.stream().forEach(info -> {
        //    logger.info(info.getUri().toASCIIString());
        //});
        TestValues tv = new TestValues();
        tv.setName(name);
        tv.setAge(20);
        //testService();
        //User user = testClient.getStores(tv);
        //System.out.println(user.getName());

        return tv.getName();
    }

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=client2";
        SpringApplication.run(ClientApplication2.class, args);
    }

}

