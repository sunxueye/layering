package org.sluck.arch.layering.backwar.service2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.layering.backwar.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Test2Application {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/hello")
    public String home() {
        logger.info("i am service 2");
        return "Hello world, i am service 2";
    }

    @RequestMapping("/user")
    public User getUser() {
        User user = new User();
        user.setName("sxy");
        user.setAge(20);
        return user;
    }

    public static void main(String[] args) {

        args = new String[1];
        args[0] = "--spring.profiles.active=regist2";
        SpringApplication.run(Test2Application.class, args);

    }

}

