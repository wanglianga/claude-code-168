package com.ruralmarket.platform.config;

import org.springframework.security.core.context.SecurityContextHolder;

/** 当前登录用户（从 JWT 解析） */
public record AuthUser(Long id, String username, String name, String role) {
    public static AuthUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        throw new IllegalStateException("未登录");
    }
}
