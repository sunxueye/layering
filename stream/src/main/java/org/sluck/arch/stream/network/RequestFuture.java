package org.sluck.arch.stream.network;

import com.google.common.util.concurrent.AbstractFuture;
import org.sluck.arch.stream.network.request.Response;

/**
 * 请求future
 *
 * author: sunxy
 * createTime: 2019/6/20:15:19
 * since: 1.0.0
 */
public class RequestFuture extends AbstractFuture<Response> {

    private int messageNumber;// 消息编号

    private RemoteCommand request; //请求命令

    private boolean success;//是否处理成功

    public RequestFuture(int messageNumber, RemoteCommand request) {
        this.request = request;
        this.messageNumber = messageNumber;
    }

    public RemoteCommand getRequest() {
        return request;
    }

    public void setResponse(Response response) {
        success = true;
        super.set(response);
    }

    public int getMessageNumber() {
        return messageNumber;
    }
}
