package com.ruralmarket.platform.config;

/** 业务异常，message 直接返回给前端展示 */
public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
