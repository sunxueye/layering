package org.sluck.arch.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.demo.ConsumerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Created by sunxy on 2019/3/25 14:22.
 */
@RestController
public class ProducerController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SendService sendService;

    @Resource
    private ConsumerTest consumerTest;

    @RequestMapping("/send/{msg}")
    public void send(@PathVariable("msg") String msg){
        logger.info("开始发送消息 --------------------- :" + msg);
//        sendService.sendMsg(msg);
        logger.info("消息发送完毕");
    }

    @RequestMapping("/check")
    private void check() {
        consumerTest.check();
    }
}
