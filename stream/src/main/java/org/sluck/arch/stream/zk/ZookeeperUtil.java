package org.sluck.arch.stream.zk;

import com.github.zkclient.ZkClient;
import com.github.zkclient.exception.ZkNodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * zk 工具
 * <p>
 * author: sunxy
 * createTime: 2019/6/19:14:57
 * since: 1.0.0
 */
public class ZookeeperUtil {

    public static final String ROOT_PATH = "/mq.coordinator";

    public static final String COORDINATOR_PATH = "/mq.coordinator/coordinator.address"; //协调者地址

    private ZkClient zkClient = null;

    private List<String> coordinatorList = new ArrayList<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) throws InterruptedException {
        ZookeeperUtil util = new ZookeeperUtil();
        util.init("192.168.1.163:2181");

        util.registAddress("192.168.1.180", 8080);

        List<String> list = util.getCoordinatorAddress();
        if (list != null) {
            System.out.println(list);
        }

        TimeUnit.SECONDS.sleep(Integer.MAX_VALUE);
    }

    /**
     * 初始化 zk 工具
     *
     * @param zkAddress
     */
    public void init(String zkAddress) {
        try {
            zkClient = new ZkClient(zkAddress, 15000, 3000);
            //订阅 协调者地址
            List<String> address = zkClient.subscribeChildChanges(COORDINATOR_PATH, (parentPath, list) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("检测到协调者地址变更:{}", list.toString());
                }

                coordinatorList.clear();
                coordinatorList.addAll(list);
            });
            if (address != null) {
                coordinatorList.addAll(address);
            }
        } catch (Exception e) {
            logger.error("获取协调者化节点异常:{}", COORDINATOR_PATH);
            throw new RuntimeException("zk 启动失败 :" + COORDINATOR_PATH, e);
        }
    }

    /**
     * 在 zk 上注册 group ip 和 端口
     *
     * @param ip
     * @param port
     */
    public void registAddress(String ip, int port) {
        if (zkClient == null) {
            throw new RuntimeException("zk 还未初始化");
        }

        String path = COORDINATOR_PATH + "/" + ip + ":" + port;

        try {
            if (zkClient.exists(path)) {
                logger.info("节点已经存在{}， 可能由于脑裂导致，稍后重试", path);
                TimeUnit.SECONDS.sleep(3);
            }
            zkClient.createEphemeral(path);
        } catch (ZkNodeExistsException e) {
            logger.error("节点已经存在，且重试后还未消失，请确定 ip 和 port 是否正确，或者等待 zk session 超时后再重试 :{}", path);
            throw new RuntimeException("zk 启动失败 :" + path, e);
        } catch (Exception e) {
            logger.error("创建临时节点异常:{}", path);
            throw new RuntimeException("zk 启动失败 :" + path, e);
        }
    }

    /**
     * 获取协调者的地址
     *
     * @return
     */
    public List<String> getCoordinatorAddress() {
        if (zkClient == null) {
            throw new RuntimeException("zk 还未初始化");
        }

        return Collections.unmodifiableList(coordinatorList);
    }

    public void close() {
        zkClient.close();
    }
}
