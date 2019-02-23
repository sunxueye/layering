package org.sluck.arch.soawar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableEurekaServer
public class SoawarApplication {

    //@Value("${layer.test}")
    //private String testValue;
    //
    //@PostConstruct
    //private void test() {
    //    System.out.println("testValue is :" + testValue);
    //}

    public static void main(String[] args) {

        SpringApplication.run(SoawarApplication.class, args);
    }

}

