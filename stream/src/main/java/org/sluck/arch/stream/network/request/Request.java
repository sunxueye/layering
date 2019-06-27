package org.sluck.arch.stream.network.request;

import org.sluck.arch.stream.network.RemoteCommand;

/**
 * 请求对象
 *
 * author: sunxy
 * createTime: 2019/6/20:17:14
 * since: 1.0.0
 */
public class Request {

    private int messageNumber; //消息 sequence

    private RemoteCommand message;//远程的报文请求信息

    public Request(int messageNumber, RemoteCommand message) {
        this.messageNumber = messageNumber;
        this.message = message;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public RemoteCommand getCommand() {
        return message;
    }
}
