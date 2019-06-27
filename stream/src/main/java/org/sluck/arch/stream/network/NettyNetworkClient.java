package org.sluck.arch.stream.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.exception.ShutdownException;
import org.sluck.arch.stream.network.message.HeartbeatMsg;
import org.sluck.arch.stream.network.message.MessageType;
import org.sluck.arch.stream.network.nettycoder.CommandProcessorDecoder;
import org.sluck.arch.stream.network.nettycoder.FastJonMsgEncoder;
import org.sluck.arch.stream.network.nettycoder.FastJsonMsgDecoder;
import org.sluck.arch.stream.network.request.Response;
import org.sluck.arch.stream.util.thread.ExecutorServiceFactory;
import org.sluck.arch.stream.util.thread.PoolThreadFactory;

import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 和协调者交互的网联客户端
 * <p>
 * author: sunxy
 * createTime: 2019/6/20:15:01
 * since: 1.0.0
 */
public class NettyNetworkClient {

    private String serverIp;

    private int serverPort;

    private AtomicInteger sequenceNumber = new AtomicInteger(0);

    private Bootstrap bootstrap = new Bootstrap();
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(1); //IO 处理线程
    private EventLoopGroup exExecutors = new LocalEventLoopGroup(10, new PoolThreadFactory("netty_handle_pool"));//业务处理线程
    private Channel activeChannel;

    private ConcurrentHashMap<Integer, RequestFuture> unCompletedRequest = new ConcurrentHashMap<>();

    private ScheduledExecutorService heartThread = ExecutorServiceFactory.newScheduledThreadPool(1, new PoolThreadFactory("netty_heartbeat", true));
    private long lastTimestamp = 0L;

    private volatile boolean shutdown;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public NettyNetworkClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    /**
     * 对连接进行初始化
     */
    public void init() {
        NettyNetworkClient client = this;
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(exExecutors, new FastJonMsgEncoder(), new FastJsonMsgDecoder(), new CommandProcessorDecoder(client));
                    }
                });

//        ChannelFuture future = bootstrap.connect(serverIp, serverPort).sync();
        //经测试，如果 socket 还未连接就开始发送数据，future 会失败，cause() 为 ClosedChannelException
        ChannelFuture future = bootstrap.connect(serverIp, serverPort).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("socket 连接建立:{}", serverIp + ":" + serverPort);
                }
            } else {
                logger.warn("socket 连接建立失败:{}, 关闭 client", serverIp + ":" + serverPort);
                close();
            }
        }); //不同步等待连接成功
        activeChannel = future.channel();

        heartThread.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            if (now - lastTimestamp > 10000) {
                RemoteCommand command = new RemoteCommand(MessageType.HEART_BEAT, new HeartbeatMsg());
                try {
                    sendMessageOneway(command);
                } catch (ShutdownException e) {
                    logger.warn("socket 已经关闭:{}", serverIp + ":" + serverPort);
                    close();
                    return;
                }
            }
            if (logger.isDebugEnabled()) {
                logger.debug("发送心跳消息至 {}", serverIp + ":" + serverPort);
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * 同步发送请求
     *
     * @param command
     * @param timeout
     * @param unit
     * @return
     * @throws TimeoutException
     */
    public Response sendMessageSyn(RemoteCommand command, long timeout, TimeUnit unit) throws TimeoutException, ShutdownException {

        RequestFuture future = doSendMessage(command);

        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            logger.warn("等待线程被中断");
        } catch (ExecutionException e) {
            logger.error("执行代码异常", e);
        } catch (TimeoutException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("执行请求超时:{}, 移除请求 future", future.getMessageNumber());
            }
            unCompletedRequest.remove(future.getMessageNumber());
            throw e;
        }
        return Response.REQ_FAIL_RESPONSE;
    }

    public void sendMessageOneway(RemoteCommand command) throws ShutdownException {
        doSendMessage(command);
    }

    private RequestFuture doSendMessage(RemoteCommand command) throws ShutdownException {
        if (shutdown) {
            throw new ShutdownException("连接已经关闭，不能发送信息:" + serverIp + ":" + serverPort);
        }

        int number = sequenceNumber.incrementAndGet();
        RequestFuture future = new RequestFuture(number, command);
        unCompletedRequest.putIfAbsent(number, future);
        activeChannel.writeAndFlush(future).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("消息请求成功:{}", number);
                }
            } else {
                logger.warn("消息发送失败:{}", number);
                logger.warn("导致发送失败的异常:", f.cause());
                RequestFuture future1 = unCompletedRequest.remove(number);
                future1.setResponse(Response.REQ_FAIL_RESPONSE);
                if (f.cause() instanceof ClosedChannelException) {
                    logger.warn("socket 已经关闭:{}", serverIp + ":" + serverPort);
                    close();
                }
            }

            //心跳报文直接 remove
            if (command.getMessageType() == MessageType.HEART_BEAT) {
                unCompletedRequest.remove(number);
            }
        });

        updateLastSendTime();
        return future;
    }

    /**
     * 接受到远程处理请求响应的结果
     */
    public void receiveRemoteResult(int messageNumber, RemoteCommand remoteCommand) {
        RequestFuture future = unCompletedRequest.get(messageNumber);
        if (future == null) {
            logger.info("future 不存在，可能已经超时");
            return;
        }

        future.setResponse(new Response(true, messageNumber, remoteCommand));
        unCompletedRequest.remove(messageNumber);
    }

    /**
     * 关闭 连接，当出现 异常时候，也可以调用此方法 清空资源
     */
    public void close() {
        if (shutdown) {
            return;
        }
        shutdown = true;

        fastFail();
        activeChannel.close();
        heartThread.shutdownNow();
        eventLoopGroup.shutdownGracefully();
        exExecutors.shutdownGracefully();
    }

    /**
     * 更新上次发送时间
     */
    private void updateLastSendTime() {
        this.lastTimestamp = System.currentTimeMillis();
    }

    /**
     * 由于通道关闭等原因，一些还未完成的请求直接快速失败
     */
    private void fastFail() {
        unCompletedRequest.values().forEach(requestFuture -> requestFuture.setResponse(Response.REQ_FAIL_RESPONSE));

        unCompletedRequest.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NettyNetworkClient that = (NettyNetworkClient) o;
        return serverPort == that.serverPort &&
                Objects.equals(serverIp, that.serverIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverIp, serverPort);
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
