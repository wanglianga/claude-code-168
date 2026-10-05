package com.ruralmarket.security;

import com.ruralmarket.domain.SysUser;

/**
 * 当前登录用户（请求线程内）
 */
public class CurrentUser {
    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    public static void set(SysUser user) { HOLDER.set(user); }
    public static SysUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public static SysUser require() {
        SysUser u = HOLDER.get();
        if (u == null) {
            throw new com.ruralmarket.common.ApiException(401, "未登录或登录已失效");
        }
        return u;
    }

    public static boolean isStaffOrAdmin() {
        SysUser u = HOLDER.get();
        return u != null && (u.getRole() == com.ruralmarket.domain.Role.STAFF
                || u.getRole() == com.ruralmarket.domain.Role.ADMIN);
    }

    public static boolean isAdmin() {
        SysUser u = HOLDER.get();
        return u != null && u.getRole() == com.ruralmarket.domain.Role.ADMIN;
    }
}
