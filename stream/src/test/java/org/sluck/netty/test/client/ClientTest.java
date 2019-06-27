package org.sluck.netty.test.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.coordinator.MqCoordinator;
import org.sluck.arch.stream.exception.MessageSendException;
import org.sluck.arch.stream.network.message.ConsumeFailRequestMsg;
import org.sluck.arch.stream.network.message.ConsumeFailResponseMsg;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.sluck.arch.stream.zk.ZookeeperUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * author: sunxy
 * createTime: 2019/6/27:14:30
 * since: 1.0.0
 */
public class ClientTest {

    private static Logger LOG = LoggerFactory.getLogger(ClientTest.class);

    public static void main(String[] args) throws InterruptedException {
        String zkAddress = "192.168.1.163:2181";
        ZookeeperUtil zookeeperUtil = new ZookeeperUtil();
        zookeeperUtil.init(zkAddress);

        MqCoordinator mqCoordinator = new MqCoordinator();
        mqCoordinator.initClient();

        for (int i = 0; i < 20; i ++) {
            ConsumeFailRequestMsg requestMsg = new ConsumeFailRequestMsg("testGroup", "testTopic", "testJson");
            try {
                ConsumeFailResponseMsg responseMsg = mqCoordinator.sendMessageSyn(requestMsg, 2, TimeUnit.SECONDS);
                LOG.info("远程请求返回结果:{}", responseMsg.getRemark());
            } catch (TimeoutException e) {
                LOG.error("请求超时", e);
            } catch (MessageSendException e) {
                LOG.error("消息发送失败", e);
            }

            TimeUnit.SECONDS.sleep(5);
        }

        mqCoordinator.close();
        ExecutorServiceFactory.shutdown();
        zookeeperUtil.close();
    }
}
