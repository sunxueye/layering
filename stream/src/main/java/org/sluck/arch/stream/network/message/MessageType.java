package org.sluck.arch.stream.network.message;

import org.sluck.arch.stream.network.processor.CommandProcessor;
import org.sluck.arch.stream.network.processor.ConsumeFailResonseProcessor;

/**
 * 报文类型
 *
 * author: sunxy
 * createTime: 2019/6/20:15:53
 * since: 1.0.0
 */
public class MessageType {

    public static final int CONSUME_FAILE_REQUEST = 10;//消费失败请求
    public static final int CONSUME_FAILE_RESPONSE = 11;//消费失败请求报文-响应

    public static final int HEART_BEAT = 20; //心跳请求

    /**
     * 根据消息类型获取对应的报文类型
     *
     * @param msg
     * @return 不存在返回 -1
     */
    public static int getTypeByMsg(Object msg) {
        if (msg instanceof ConsumeFailRequestMsg) {
            return CONSUME_FAILE_REQUEST;
        }
        if (msg instanceof ConsumeFailResponseMsg) {
            return CONSUME_FAILE_RESPONSE;
        }
        if (msg instanceof HeartbeatMsg) {
            return HEART_BEAT;
        }

        return -1;
    }

    public static Class<?> getMsgClassByType(int messageType) {
        switch (messageType) {
            case CONSUME_FAILE_REQUEST:
                return ConsumeFailRequestMsg.class;

            case CONSUME_FAILE_RESPONSE:
                return ConsumeFailResponseMsg.class;

            case HEART_BEAT:
                return HeartbeatMsg.class;

            default:
        }

        return null;
    }

    public static CommandProcessor getProcessorByType(int messageType) {
        switch (messageType) {
            case CONSUME_FAILE_REQUEST:
                break;
            case CONSUME_FAILE_RESPONSE:
                return new ConsumeFailResonseProcessor();

            case HEART_BEAT:

            default:
        }

        return null;
    }
}
