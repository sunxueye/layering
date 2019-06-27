package org.sluck.arch.stream.network;

/**
 * 远程命令
 *
 * author: sunxy
 * createTime: 2019/6/20:16:35
 * since: 1.0.0
 */
public class RemoteCommand {

    private int messageType;

    private Object message;// 报文

    public RemoteCommand(int messageType, Object message) {
        this.messageType = messageType;
        this.message = message;
    }

    public int getMessageType() {
        return messageType;
    }

    public Object getMessage() {
        return message;
    }
}
