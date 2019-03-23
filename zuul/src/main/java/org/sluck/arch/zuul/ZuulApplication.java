package org.sluck.arch.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * Created by sunxy on 2019/3/18 20:19.
 */
@SpringBootApplication
@EnableZuulProxy
public class ZuulApplication {

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=zuul";
        SpringApplication.run(ZuulApplication.class, args);
    }
}
