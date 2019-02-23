package org.sluck.arch.soawar.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.soawar.ribbon.TestRibbonConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.List;

@SpringBootApplication
//@EnableEurekaClient
//@EnableDiscoveryClient
@RestController
@EnableFeignClients
@RibbonClient(name = "TestService", configuration = {TestRibbonConfigure.class})
public class ClientApplication {

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setReadTimeout(Duration.ofMillis(5000))
                .setConnectTimeout(Duration.ofMillis(2000))
                .build();
    }

    @Resource
    private EurekaDiscoveryClient discoveryClient;

    @Autowired
    private RestTemplate restTemplate;

    @Resource
    private TestClient testClient;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/hello")
    public void serviceUrl() {
        //List<ServiceInstance> list = discoveryClient.getInstances("TestService");
        //list.stream().forEach(info -> {
        //    logger.info(info.getUri().toASCIIString());
        //});
        User user = testClient.getStores();
        System.out.println(user.getName());
    }

    @RequestMapping("/hello2")
    public void testService() {
        logger.info("---------------------- 准备开始远程调用 ----------------------");
        String res = restTemplate.getForEntity("http://TestService/hello", String.class).getBody();
        logger.info("---------------------- 请求远程调用返回结果 --------------------:" + res);
    }

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=client";
        SpringApplication.run(ClientApplication.class, args);
    }

}

