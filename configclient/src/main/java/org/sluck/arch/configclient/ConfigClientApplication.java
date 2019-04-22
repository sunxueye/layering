package org.sluck.arch.configclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableEurekaClient
@RestController
public class ConfigClientApplication {

    @Value("${layer.test:default}")
    private String testValue;

    @RequestMapping("/hello")
    @ResponseBody
    private String test() {
        return testValue;
    }

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=configC";
        SpringApplication.run(ConfigClientApplication.class, args);
    }
}

