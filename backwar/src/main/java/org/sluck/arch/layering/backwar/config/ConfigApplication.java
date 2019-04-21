package org.sluck.arch.layering.backwar.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableConfigServer
@EnableEurekaClient
public class ConfigApplication {

    public static void main(String[] args) {

//        args = new String[1];
//        args[0] = "--spring.profiles.active=dev";
        SpringApplication.run(ConfigApplication.class, args);

    }

}

