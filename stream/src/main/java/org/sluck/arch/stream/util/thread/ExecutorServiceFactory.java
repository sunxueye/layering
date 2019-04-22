package org.sluck.arch.stream.util.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工厂，封装一层，用于 shutdown 回收资源
 * <p>
 * Created by sunxy on 2019/1/21 10:57.
 */
public class ExecutorServiceFactory {

    private static HashMap<Integer, ExecutorService> idAndService = new HashMap<>();

    private static AtomicInteger id = new AtomicInteger(0);

    private static volatile boolean shutdown = false;

    private static Logger LOG = LoggerFactory.getLogger(ExecutorServiceFactory.class);

    /**
     * 新建固定线程数量的线程池
     *
     * @param nThreads
     * @param threadFactory
     * @return
     */
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        checkShutdown();
        ExecutorService service = Executors.newFixedThreadPool(nThreads, threadFactory);
        putMap(service);
        return service;
    }

    /**
     * 新建线程数量不固定的线程池
     *
     * @param threadFactory
     * @return
     */
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        checkShutdown();
        ExecutorService service = Executors.newCachedThreadPool(threadFactory);
        putMap(service);
        return service;
    }

    /**
     * 新建单个线程的线程池
     *
     * @param threadFactory
     * @return
     */
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        checkShutdown();
        ExecutorService service = Executors.newSingleThreadExecutor(threadFactory);
        putMap(service);
        return service;
    }

    /**
     * 创建指定数量的且执行周期性任务的线程池
     *
     * @param corePoolSize
     * @param threadFactory
     * @return
     */
    public static ScheduledExecutorService newScheduledThreadPool(
            int corePoolSize, ThreadFactory threadFactory) {
        checkShutdown();
        ScheduledExecutorService service = Executors.newScheduledThreadPool(corePoolSize, threadFactory);
        putMap(service);
        return service;
    }

    /**
     * 将创建的 exeservice 放入 map
     *
     * @param service
     */
    private static void putMap(ExecutorService service) {
        Integer newID = id.incrementAndGet();
        idAndService.put(newID, service);
    }

    private static void checkShutdown() {
        if (shutdown) {
            throw new IllegalStateException("线程池工厂已经关闭");
        }
    }

    /**
     * 线程池数量
     *
     * @return
     */
    public static int serviceSize() {
        return id.get();
    }

    public static void shutdown() {
        if (shutdown) {
            return;
        }
        LOG.info("开始关闭线程池工厂....");
        shutdown = true;
        idAndService.values().stream().filter(p -> !p.isShutdown()).forEach(ExecutorService::shutdown);
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService service1 = newFixedThreadPool(1, new PoolThreadFactory("fixed"));
        service1.submit(() -> {
            int i = 0;
            while (i < 5) {
                LOG.info("fixed run...");
                System.out.println("fixed run...");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        });

        ExecutorService service2 = newCachedThreadPool(new PoolThreadFactory("cached"));
        service2.submit(() -> {
            int i = 0;
            while (i < 5) {
                LOG.info("cached run...");
                System.out.println("cached run...");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        });

        ExecutorService service3 = newSingleThreadExecutor(new PoolThreadFactory("single"));
        service3.submit(() -> {
            int i = 0;
            while (i < 5) {
                LOG.info("single run...");
                System.out.println("single run...");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                i++;
            }
        });

        ScheduledExecutorService service4 = newScheduledThreadPool(1, new PoolThreadFactory("sched"));
        service4.scheduleAtFixedRate(() -> {
            LOG.info("sched run...");
            System.out.println("sched run...");
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.SECONDS);

        TimeUnit.SECONDS.sleep(5);

        LOG.info("线程池数量：" + serviceSize());
        System.out.println("线程池数量：" + serviceSize());

        System.out.println("shutdown...");
        shutdown();
    }
}
