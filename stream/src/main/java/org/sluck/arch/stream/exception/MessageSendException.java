package org.sluck.arch.stream.exception;

/**
 * 请求发送报文出现异常，报文未发送成功
 *
 * author: sunxy
 * createTime: 2019/6/25:17:25
 * since: 1.0.0
 */
public class MessageSendException extends Exception{

    public MessageSendException(String message) {
        super(message);
    }

    public MessageSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
