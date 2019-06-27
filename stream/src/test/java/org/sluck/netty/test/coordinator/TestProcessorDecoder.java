package org.sluck.netty.test.coordinator;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.network.RemoteCommand;
import org.sluck.arch.stream.network.RequestFuture;
import org.sluck.arch.stream.network.message.ConsumeFailResponseMsg;
import org.sluck.arch.stream.network.message.MessageType;
import org.sluck.arch.stream.network.request.Request;

import java.util.HashSet;
import java.util.Set;

/**
 *
 *
 * author: sunxy
 * createTime: 2019/6/20:18:53
 * since: 1.0.0
 */
public class TestProcessorDecoder extends ChannelInboundHandlerAdapter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Set<Integer> msgSet = new HashSet<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Request) {
            Request request = (Request) msg;
            int msgType = request.getCommand().getMessageType();
            msgSet.add(request.getMessageNumber());

            if (MessageType.CONSUME_FAILE_REQUEST == msgType) {
                logger.info("接受到消息消费失败请求:{}", request.getMessageNumber());
                RemoteCommand command = new RemoteCommand(MessageType.CONSUME_FAILE_RESPONSE, new ConsumeFailResponseMsg(true, "save success"));
                RequestFuture requestFuture = new RequestFuture(request.getMessageNumber(), command);
                ctx.channel().writeAndFlush(requestFuture).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("发送响应消息成功:{}", request.getMessageNumber());
                        }
                    } else {
                        logger.warn("发送响应消息失败：{}", request.getMessageNumber(), future.cause());
                    }
                });

            } else if (MessageType.HEART_BEAT == msgType) {
                if (logger.isDebugEnabled()) {
                    logger.debug("接受到心跳请求报文:{}", request.getMessageNumber());
                }
            } else {
                logger.error("未知的报文类型:{}", msgType);
            }
        }

        if (msgSet.size() % 1000 == 0) {
            logger.info("接受到的消息数量:{}", msgSet.size());
        }
        ctx.fireChannelRead(msg);
    }

    public Set<Integer> getMsgSet() {
        return msgSet;
    }
}
