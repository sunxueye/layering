package org.sluck.arch.stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by sunxy on 2019/3/25 14:22.
 */
@RestController
public class ProducerController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SendService sendService;

    @RequestMapping("/send/{msg}")
    public void send(@PathVariable("msg") String msg){
        logger.info("开始发送消息 --------------------- :" + msg);
        sendService.sendMsg(msg);
        logger.info("消息发送完毕");
    }
}
