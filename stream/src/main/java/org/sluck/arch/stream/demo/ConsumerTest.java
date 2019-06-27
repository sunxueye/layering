package org.sluck.arch.stream.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.annotation.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * author: sunxy
 * createTime: 2019/6/3:16:18
 * since: 1.0.0
 */
@Component
public class ConsumerTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Integer, Integer> c1 = new ConcurrentHashMap<>();
    private Map<Integer, Integer> c2 = new ConcurrentHashMap<>();

    @EventListener(topic = "testTopic")
    public boolean consume1(HelloMsg helloMsg) {
        logger.info("consume 1 : hello, my name is {}, and msg is {}", helloMsg.getName(), helloMsg.getMsg());
        return setMap(helloMsg, c1);
    }

    @EventListener(topic = "testTopic")
    public boolean consume2(HelloMsg helloMsg) {
        logger.info("consume2 : hello, my name is {}, and msg is {}", helloMsg.getName(), helloMsg.getMsg());
        return setMap(helloMsg, c2);
    }

    private boolean setMap(HelloMsg helloMsg, Map<Integer, Integer> c1) {
        String num = helloMsg.getMsg().split(":")[1];
        Integer key = Integer.valueOf(num);
        Integer old = c1.putIfAbsent(key, 1);
        if (old != null) {
            c1.put(key, old + 1);
        }
        return true;
    }

    public void check() {
        logger.info("开始检查 c1 :");
        doCheck(c1);

        logger.info("开始检查 c2 :");
        doCheck(c2);
    }

    private void doCheck(Map<Integer, Integer> map) {
        logger.info("map size is {}", map.size());
        int size = map.size();
        for (int i = 0; i < size; i++) {
            if (map.get(i) == null) {
                logger.info("msg is not exist : {}", i);
            } else if (map.get(i) > 1) {
                logger.info("msg count is > 1 : {}", map.get(i));
            }
        }
    }

    public static class HelloMsg {

        private String name;

        private String msg;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}
