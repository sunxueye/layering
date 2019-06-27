package org.sluck.arch.stream.binder.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于保存 topic_channel 对应的还未消费的 offset
 * 当获取可提交的 offset 的时候，直接获取最小的 key 即可（此动作无需上锁）
 * <p>
 * Created by sunxy on 2019/3/28 17:43.
 */
public class OffsetCommitQueue {

    private ReentrantLock lock = new ReentrantLock();//并发操作的锁，此对象会被多个线程并发操作，直接上重量级锁，跳过 synchronized 的轻量级与偏向锁

    private TreeSet<Long> offsets = new TreeSet<>();// 保存了该 channel 的还未消费的 offset

    private Long maxOffset; //用于保存此队列最大的 offset，防止 queue 所有的值都被取走无值的情况下没有 offset 可提交

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 添加 offset
     *
     * @param offsetList
     */
    public void addOffset(List<Long> offsetList) {
        try {
            lock.lock();

            offsetList.forEach(offset -> {
                offsets.add(offset);
                if (maxOffset == null || offset > maxOffset) {
                    maxOffset = offset;
                }
            });
        } catch (Exception | Error e) {
            logger.error("offset 增加异常", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 移除 offset
     *
     * @param offset
     */
    public void removeOffset(Long offset) {
        try {
            lock.lock();

            offsets.remove(offset);
        } catch (Exception | Error e) {
            logger.error("offset 移除异常", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取需要提交的 offset
     *
     * @return 如果刚初始化，还未有 offset，则返回 null
     */
    public Long getNeedCommitOffset() {
        long max = maxOffset; //先保存下副本到线程栈中，防止并并发修改
        try {
            return offsets.first();
        } catch (NoSuchElementException e) {
            return max;
        }
    }

    /**
     * 是否还包含未提交的 offset
     *
     * @return
     */
    public boolean hasNoCommitOffset() {
        try {
            return offsets.first() != null;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

}
