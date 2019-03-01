package org.sluck.arch.soawar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class RegistServer3Application {

    //@Value("${layer.test}")
    //private String testValue;
    //
    //@PostConstruct
    //private void test() {
    //    System.out.println("testValue is :" + testValue);
    //}

    public static void main(String[] args) {

        args = new String[1];
        args[0] = "--spring.profiles.active=server3";
        SpringApplication.run(RegistServer3Application.class, args);
    }

}
