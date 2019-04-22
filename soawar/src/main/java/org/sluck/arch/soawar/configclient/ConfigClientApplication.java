package org.sluck.arch.soawar.configclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
//@EnableEurekaServer
@RestController
@EnableDiscoveryClient
public class ConfigClientApplication {

//    @Value("${layer.test:default}")
//    private String testValue;
//
//    @RequestMapping("/hello")
//    @ResponseBody
//    private String test() {
//        return testValue;
//    }

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=configC";
        SpringApplication.run(ConfigClientApplication.class, args);
    }

}

