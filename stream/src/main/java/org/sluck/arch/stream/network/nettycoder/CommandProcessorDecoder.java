package org.sluck.arch.stream.network.nettycoder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.network.NettyNetworkClient;
import org.sluck.arch.stream.network.request.Request;
import org.sluck.arch.stream.network.message.MessageType;
import org.sluck.arch.stream.network.processor.CommandProcessor;

/**
 * 用于解析出命令，并转发至对应的处理器
 *
 * author: sunxy
 * createTime: 2019/6/20:18:53
 * since: 1.0.0
 */
public class CommandProcessorDecoder extends ChannelInboundHandlerAdapter {

    private NettyNetworkClient networkClient;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public CommandProcessorDecoder(NettyNetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Request) {
            Request request = (Request) msg;
            int msgType = request.getCommand().getMessageType();

            CommandProcessor processor = MessageType.getProcessorByType(msgType);
            if (processor == null) {
                logger.error("不存在的命令对应的处理器 {}", msgType );
            } else {
                processor.handle(networkClient, request);
            }
        }
        ctx.fireChannelRead(msg);
    }


}
