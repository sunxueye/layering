package org.sluck.arch.stream.network.nettycoder;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.sluck.arch.stream.network.RemoteCommand;
import org.sluck.arch.stream.network.request.Request;
import org.sluck.arch.stream.network.message.MessageType;

import java.util.List;

/**
 * author: sunxy
 * createTime: 2019/6/19:19:30
 * since: 1.0.0
 */
public class FastJsonMsgDecoder extends ByteToMessageDecoder {

    private static final int HEAD_LENGTH = 4;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < HEAD_LENGTH) {  //这个HEAD_LENGTH是我们用于表示头长度的字节数。  由于Encoder中我们传的是一个int类型的值，所以这里HEAD_LENGTH的值为4.
            return;
        }
        in.markReaderIndex();                  //我们标记一下当前的readIndex的位置
        int dataLength = in.readInt();       // 读取传送过来的消息的长度。ByteBuf 的readInt()方法会让他的readIndex增加4
        if (dataLength < 0) { // 我们读到的消息体长度为0，这是不应该出现的情况，这里出现这情况，关闭连接。
            ctx.close();
        }

        if (in.readableBytes() < dataLength) { //读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex. 这个配合markReaderIndex使用的。把readIndex重置到mark的地方
            in.resetReaderIndex();
            return;
        }

        int messageNumber = in.readInt();//报文编号
        int messageType = in.readInt();//报文类型

        byte[] body = new byte[dataLength - 4 - 4];  //传输正常
        in.readBytes(body);

        Object o = convertToObject(messageType, body);  //将byte数据转化为我们需要的对象

        Request request = new Request(messageNumber, new RemoteCommand(messageType, o));
        out.add(request);
    }

    private Object convertToObject(int msgType, byte[] body) {

        String json = new String(body);
        Class<?> type = MessageType.getMsgClassByType(msgType);
        if (type == null) {
            throw new RuntimeException("不存在的报文类型：" + msgType);
        }
        return JSON.parseObject(json, type);
    }
}
