package org.sluck.arch.soawar.registserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableEurekaServer
@RestController
public class RegistServer2Application {

    //@Value("${layer.cglibproxytest}")
    //private String testValue;
    //
    //@PostConstruct
    //private void cglibproxytest() {
    //    System.out.println("testValue is :" + testValue);
    //}

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=server2";
        SpringApplication.run(RegistServer2Application.class, args);
    }

}

