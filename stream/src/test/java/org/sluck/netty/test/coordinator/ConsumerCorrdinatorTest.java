package org.sluck.netty.test.coordinator;

import com.github.zkclient.ZkClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.sluck.arch.stream.network.nettycoder.FastJonMsgEncoder;
import org.sluck.arch.stream.network.nettycoder.FastJsonMsgDecoder;
import org.sluck.arch.stream.zk.ZookeeperUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * author: sunxy
 * createTime: 2019/6/27:14:09
 * since: 1.0.0
 */
public class ConsumerCorrdinatorTest {

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        String zkAddress = "192.168.1.163:2181";
        ZkClient zkClient = new ZkClient(zkAddress, 15000, 3000);
        if (!zkClient.exists(ZookeeperUtil.COORDINATOR_PATH)) {
            if (!zkClient.exists(ZookeeperUtil.ROOT_PATH)) {
                zkClient.createPersistent(ZookeeperUtil.ROOT_PATH);
            }
            zkClient.createPersistent(ZookeeperUtil.COORDINATOR_PATH);
        }
        ZookeeperUtil zookeeperUtil = new ZookeeperUtil();
        zookeeperUtil.init(zkAddress);
        String ip = InetAddress.getLocalHost().getHostAddress();
        zookeeperUtil.registAddress(ip, 8089);

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new FastJonMsgEncoder(),
                                    new FastJsonMsgDecoder(),
                                    new TestProcessorDecoder());
                        }
                    });

            // Bind and start to accept incoming connections.
            b.bind(8089).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            zkClient.close();
            zookeeperUtil.close();
        }


    }
}
