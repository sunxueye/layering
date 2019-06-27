package org.sluck.arch.stream.network.processor;

import org.sluck.arch.stream.network.NettyNetworkClient;
import org.sluck.arch.stream.network.request.Request;
import org.sluck.arch.stream.network.message.MessageType;

/**
 * 消费失败请求结果响应处理器
 *
 * author: sunxy
 * createTime: 2019/6/20:19:03
 * since: 1.0.0
 */
public class ConsumeFailResonseProcessor implements CommandProcessor{

    @Override
    public void handle(NettyNetworkClient client, Request request) {
        if (request.getCommand().getMessageType() != MessageType.CONSUME_FAILE_RESPONSE) {
            throw new RuntimeException("消息类型不匹配, 期望:" + MessageType.CONSUME_FAILE_RESPONSE + ", 接收:" + request.getCommand().getMessageType());
        }

        client.receiveRemoteResult(request.getMessageNumber(), request.getCommand());
    }
}
