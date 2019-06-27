package org.sluck.arch.stream.coordinator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.exception.MessageSendException;
import org.sluck.arch.stream.exception.ShutdownException;
import org.sluck.arch.stream.network.NettyNetworkClient;
import org.sluck.arch.stream.network.RemoteCommand;
import org.sluck.arch.stream.network.message.MessageType;
import org.sluck.arch.stream.network.message.RequestMessage;
import org.sluck.arch.stream.network.message.ResponseMessage;
import org.sluck.arch.stream.network.request.Response;
import org.sluck.arch.stream.zk.ZookeeperUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 在 client 端模拟消费协调者与 client 交互
 * <p>
 * author: sunxy
 * createTime: 2019/6/20:15:03
 * since: 1.0.0
 */
public class MqCoordinator {

    @Resource
    private ZookeeperUtil zookeeperUtil;
    private int nextCoorIndex; //下一个协调者的 index

    private Map<String, NettyNetworkClient> clientMap = new HashMap<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 初始化客户端
     */
    public void initClient() {
        List<String> clients = zookeeperUtil.getCoordinatorAddress();
        if (clients.isEmpty()) {
            throw new RuntimeException("无可用的 mq 协调者地址");
        }

        for (String s : clients) {
            if (!clientMap.containsKey(s)) {
                clientMap.computeIfAbsent(s, this::createClient);
            }
        }
    }

    /**
     * 同步发送请求，并等待响应结果
     *
     * @param message
     * @param <R>     返回类型
     * @param <Q>请求类型
     * @return
     */
    public <R extends ResponseMessage, Q extends RequestMessage> R sendMessageSyn(Q message, long timeout, TimeUnit unit) throws TimeoutException, MessageSendException {
        List<String> address = zookeeperUtil.getCoordinatorAddress();
        updateClientMap(address);

        String nextKey = getCoordinatorAddress(address);
        if (nextKey == null) {
            throw new MessageSendException("无可用的 mq 协调者地址");
        }
        NettyNetworkClient client = clientMap.get(nextKey);
        if (client == null) {
            throw new MessageSendException("无可用的 mq 协调者客户端");
        }
        int msgType = MessageType.getTypeByMsg(message);
        if (msgType < 0) {
            throw new IllegalArgumentException("不存在的消息类型:" + message.getClass().getSimpleName());
        }
        RemoteCommand remoteCommand = new RemoteCommand(msgType, message);
        try {
            Response response = client.sendMessageSyn(remoteCommand, timeout, unit);
            if (!response.isRequestSuccess()) {
                throw new MessageSendException("消息未发送成功");
            }
            Class returnType = MessageType.getMsgClassByType(response.getCommand().getMessageType());
            if (!returnType.isInstance(response.getCommand().getMessage())) {
                throw new IllegalArgumentException("返回的消息类型: " + returnType.getSimpleName() + " 和预期的消息类型不匹配");
            }
            return (R) response.getCommand().getMessage();
        } catch (ShutdownException e) {
            logger.error("client 已经关闭, 准备重试新的地址");
            client.close();
            return sendMessageSyn(message, timeout, unit);
        }
    }

    /**
     * 更新协调者 client
     *
     * @param newCoordAddress
     */
    private void updateClientMap(List<String> newCoordAddress) {
        //新增的地址或失效的进行添加
        newCoordAddress.forEach(address -> {
            if (!clientMap.containsKey(address) || clientMap.get(address).isShutdown()) {
                clientMap.computeIfAbsent(address, this::createClient);
            }
        });

        //筛选出需要清楚的无用 client 地址
        List<String> needClearKey = new ArrayList<>();
        clientMap.keySet().forEach(address -> {
            if (!newCoordAddress.contains(address) || clientMap.get(address).isShutdown()) {
                needClearKey.add(address);
            }
        });
        //clear
        needClearKey.forEach(address -> {
            clientMap.get(address).close();
            clientMap.remove(address);
        });
    }

    /**
     * 根据地址创建 client
     *
     * @param address ip:port
     * @return 如果创建失败 返回 null
     */
    private NettyNetworkClient createClient(String address) {
        try {
            String[] as = address.split(":");
            String ip = as[0];
            int port = Integer.valueOf(as[1]);
            NettyNetworkClient client = new NettyNetworkClient(ip, port);
            client.init();

            return client;
        } catch (Exception e) {
            logger.warn("初始化 netty client 失败 ：{}，异常：{}", address, e.getMessage());
        }

        return null;
    }

    /**
     * 获取下一个可用的协调者的地址
     *
     * @param coordinatorList
     * @return
     */
    private String getCoordinatorAddress(List<String> coordinatorList) {
        if (coordinatorList == null || coordinatorList.isEmpty()) {
            return null;
        }
        int size = coordinatorList.size();
        int index = nextCoorIndex % size;
        nextCoorIndex++;
        return coordinatorList.get(index);
    }

    public void close() {
        clientMap.values().forEach(NettyNetworkClient::close);
    }
}
