package org.sluck.arch.stream.util.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池类的线程工厂
 * <p>
 * Created by sunxy on 2017/10/31 15:21.
 */
public class PoolThreadFactory implements ThreadFactory {

    private String poolName;

    private boolean daemon = false;

    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);

    public PoolThreadFactory(String poolName) {
        this.poolName = poolName;
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
    }

    public PoolThreadFactory(String poolName, boolean daemon) {
        this(poolName);
        this.daemon = daemon;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                poolName + "-" + threadNumber.getAndIncrement(),
                0);
        t.setDaemon(daemon);
        return t;
    }
}
