package org.sluck.arch.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.annotation.EnableEventHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created by sunxy on 2019/3/18 14:09.
 */
@SpringBootApplication
@EnableEventHandler
public class StreamApplication {

    private static Logger LOG = LoggerFactory.getLogger(StreamApplication.class);

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=stream1";
        try {
            SpringApplication.run(StreamApplication.class, args);
        } catch (Exception | Error e) {
            e.printStackTrace();
            LOG.error("启动异常", e);
        }
    }
}
