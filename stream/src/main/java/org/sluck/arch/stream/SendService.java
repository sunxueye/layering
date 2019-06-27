package org.sluck.arch.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by sunxy on 2019/3/25 14:21.
 */
@Component
//@EnableBinding(value = {StreamClient.class})
public class SendService {

    private Logger logger = LoggerFactory.getLogger(getClass());

//    @Autowired
//    private StreamClient source;
//
//    public void sendMsg(String msg){
//        Person p = new Person();
//        p.name = msg;
//        source.output().send(MessageBuilder.withPayload(p).build());
//    }
//
//    @StreamListener(StreamClient.INPUT)
//    @EventListener(topic = "my-replicated-topic2")
//    public void receive(Person message) {
//        logger.info("StreamReceiver: {}", message.getName());
//    }
//
//    public static class Person {
//        private String name;
//        public String getName() {
//            return name;
//        }
//        public void setName(String name) {
//            this.name = name;
//        }
//        public String toString() {
//            return this.name;
//        }
//    }

}
