package org.sluck.arch.layering.backwar.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.api.TestClient;
import org.sluck.arch.api.TestValues;
import org.sluck.arch.api.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

//import org.sluck.arch.layering.backwar.User;

@SpringBootApplication
@RestController
public class  TestApplication implements TestClient {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/hello")
    public String home() {
        logger.info("i am service 1, slepping ");
        //try {
        //    Thread.currentThread().sleep(4000);
        //} catch (InterruptedException e) {
        //    e.printStackTrace();
        //}

        return "Hello world, i am service 1";
    }

    //@RequestMapping("/user")
    //public User getUser() {
    //    User user = new User();
    //    user.setName("sxy");
    //    user.setAge(20);
    //    return user;
    //}

    public static void main(String[] args) {

        args = new String[1];
        args[0] = "--spring.profiles.active=regist";
        SpringApplication.run(TestApplication.class, args);

    }

    @Override
    @RequestMapping("/user")
    public User getStores(@RequestBody TestValues testValues) {
        logger.info("user Service begin....:" + testValues.getName());
        User user = new User();
        user.setName(testValues.getName() + "1");
        user.setAge(testValues.getAge() + 1);
        return user;
    }
}

