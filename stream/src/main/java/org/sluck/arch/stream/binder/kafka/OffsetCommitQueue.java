package org.sluck.arch.stream.binder.kafka;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于保存可以提交的最小的 offset 的队列，
 * 如： 1 2 4 5 8, 最小的就是 2，
 * 当插入 3 的时候，就变成 5，
 * <p> 最少会保存最后一次提交的值
 * Created by sunxy on 2019/3/28 17:43.
 */
public class OffsetCommitQueue {

    private ReentrantLock lock = new ReentrantLock();

    private Long minOffset;

    private volatile boolean needCommit; //是否需要 提交，当 minOffset 变更时候需要

    private PriorityQueue<Long> otherOffsets = new PriorityQueue<>();

    public static void main(String[] args) throws InterruptedException {

        ExecutorService service = Executors.newFixedThreadPool(5);
        OffsetCommitQueue queue = new OffsetCommitQueue();
        Random random = new Random();

        Set<Long> set = new HashSet<>();

        int i = 5;
        while(i > 0) {
            Runnable run = () -> {

                for (int j = 0; j < 10000; j ++) {
                    Long value = Long.valueOf(random.nextInt(10000));
                    queue.add(value);
                    set.add(value);
                }

                Long begin = System.currentTimeMillis();
                for (int j = 0; j < 10000; j ++) {
                    Long value = Long.valueOf(random.nextInt(10000));
                    queue.add(value);
                    set.add(value);
                }

                System.out.println("end :" + (System.currentTimeMillis() - begin) + " ms");
            };
            service.submit(run);
            i--;
        }

        Thread.currentThread().sleep(1000);

        while (queue.isNeedCommit()) {
            Long value = queue.poll();
            System.out.println(value);
        }

        StringBuilder sb = new StringBuilder();
        for (int t=0; t<10000;t++ ) {
            if (!set.contains(Long.valueOf(t))) {
                sb.append(t + ",");
            }

        }
        //set.forEach(l -> {
        //    if ()
        //
        //});
        System.out.println(sb);
    }

    /**
     * 增加已经提交的Offset
     *
     * @param offset
     */
    public void add(Long offset) {

        try {
            lock.lock();

            if (minOffset == null) {
                updateMinOffset(offset);
                return;
            }

            Long next = otherOffsets.peek();
            //去重
            if (offset.equals(minOffset) || offset.equals(next)) {
                return;
            }

            if (offset <= minOffset) {
                //donothing
            } else if (offset - minOffset > 1) {
                addQueueOffset(offset);
            } else {
                //正好 多 1
                if (!otherOffsets.isEmpty()) {
                    if (next - offset == 1) {
                        //需要从 队列中获取 最小的 offset
                        Long newMin = pollQueueOffset();
                        if (newMin != null) {
                            updateMinOffset(newMin);
                            return;
                        }
                    }
                }
                updateMinOffset(offset);
            }

        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取可以提交的 offset，如果为空 返回 null
     *
     * @return
     */
    public Long poll() {
        if (minOffset == null) {
            return null;
        }

        try {
            lock.lock();

            if (otherOffsets.isEmpty()) {
                needCommit = false;
                return minOffset;
            }

            Long returnValue = minOffset;
            needCommit = false;

            Long next = otherOffsets.peek();
            if (next - minOffset == 1) {
                Long newMin = pollQueueOffset();
                if (newMin != null) {
                    updateMinOffset(newMin);
                }
            }

            return returnValue;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 添加新的 offset 至队列中
     *
     * @param newOffset
     * @return
     */
    private void addQueueOffset(Long newOffset) {

        if (otherOffsets.isEmpty()) {
            otherOffsets.add(newOffset);
            return;
        }

        Long first = otherOffsets.peek();
        if (!newOffset.equals(first) && first - newOffset != 1) {
            otherOffsets.add(newOffset);
        }
    }

    /**
     * 从优先级队列中获取最小的可提交的 offset，如果没有则为 null
     *
     * @return
     */
    private Long pollQueueOffset() {
        if (otherOffsets.isEmpty()) {
            return null;
        }

        Long first = otherOffsets.poll();
        Long next = otherOffsets.peek();
        if (next == null) {
            return first;
        } else if (next - first > 1) {
            return first;
        } else {
            //等于 或 正好大1 的情况 递归继续处理
            return pollQueueOffset();

        }
    }

    public boolean isNeedCommit() {
        return needCommit;
    }

    private void updateMinOffset(Long min) {
        this.minOffset = min;
        this.needCommit = true;
    }

}
