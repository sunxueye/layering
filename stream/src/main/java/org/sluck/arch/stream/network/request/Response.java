package org.sluck.arch.stream.network.request;

import org.sluck.arch.stream.network.RemoteCommand;

/**
 * 请求结果响应对象
 * 如果请求失败，则此对象不一定包含返回报文
 *
 * author: sunxy
 * createTime: 2019/6/20:17:14
 * since: 1.0.0
 */
public class Response {

    public static Response REQ_FAIL_RESPONSE = new Response(false);

    private boolean requestSuccess; //请求是否成功

    private int messageNumber; //消息 sequence

    private RemoteCommand message;//远程返回的报文信息

    private Response(boolean success) {
        this.requestSuccess = success;
    }

    public Response(boolean requestSuccess, int messageNumber, RemoteCommand message) {
        this.requestSuccess = requestSuccess;
        this.messageNumber = messageNumber;
        this.message = message;
    }

    public boolean isRequestSuccess() {
        return requestSuccess;
    }

    public int getMessageNumber() {
        return messageNumber;
    }

    public RemoteCommand getCommand() {
        return message;
    }
}
