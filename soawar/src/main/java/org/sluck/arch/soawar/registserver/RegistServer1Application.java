package org.sluck.arch.soawar.registserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class RegistServer1Application {

    //@Value("${layer.cglibproxytest}")
    //private String testValue;
    //
    //@PostConstruct
    //private void cglibproxytest() {
    //    System.out.println("testValue is :" + testValue);
    //}

    public static void main(String[] args) {

        args = new String[1];
        args[0] = "--spring.profiles.active=server1";
        SpringApplication.run(RegistServer1Application.class, args);
    }

}

