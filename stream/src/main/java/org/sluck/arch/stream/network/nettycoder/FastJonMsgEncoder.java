package org.sluck.arch.stream.network.nettycoder;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.sluck.arch.stream.network.RequestFuture;

/**
 * 将报文请求转为 byte 发送，报文内容：
 * int（后面的长度） + int(报文编号） + int(报文类型) + byte[] （报文内容）
 *
 *
 * author: sunxy
 * createTime: 2019/6/19:19:28
 * since: 1.0.0
 */
public class FastJonMsgEncoder extends MessageToByteEncoder<RequestFuture> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RequestFuture msg, ByteBuf out) throws Exception {

        int messageNumber = msg.getMessageNumber();
        int messageType = msg.getRequest().getMessageType();
        Object message = msg.getRequest().getMessage();

        byte[] body = convertToBytes(message);  //将对象转换为byte

        int dataLength = body.length;  //读取消息的长度
        out.writeInt(dataLength + 4 + 4);  //2 个 int + 报文内容
        out.writeInt(messageNumber);  //报文编号
        out.writeInt(messageType);  //报文内容
        out.writeBytes(body);  //消息体中包含我们要发送的数据
    }

    private byte[] convertToBytes(Object message) {
        return JSON.toJSONString(message).getBytes();
    }
}
