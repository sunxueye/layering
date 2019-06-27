package org.sluck.arch.stream.exception;

/**
 * client 关闭异常
 *
 * author: sunxy
 * createTime: 2019/6/25:17:18
 * since: 1.0.0
 */
public class ShutdownException extends Exception {

    public ShutdownException(String message) {
        super(message);
    }
}
