package org.sluck.arch.layering.backwar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class BackwarApplication {

    public static void main(String[] args) {

        args = new String[1];
        args[0] = "--spring.profiles.active=dev";
        SpringApplication.run(BackwarApplication.class, args);

    }

}

