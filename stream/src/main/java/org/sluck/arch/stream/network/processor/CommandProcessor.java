package org.sluck.arch.stream.network.processor;

import org.sluck.arch.stream.network.NettyNetworkClient;
import org.sluck.arch.stream.network.request.Request;

/**
 * 用于处理命令请求的处理器
 *
 * author: sunxy
 * createTime: 2019/6/21:15:14
 * since: 1.0.0
 */
public interface CommandProcessor {

    /**
     * 对给定的 client 的 请求进行处理
     *
     * @param client
     * @param request
     */
    void handle(NettyNetworkClient client, Request request);
}
