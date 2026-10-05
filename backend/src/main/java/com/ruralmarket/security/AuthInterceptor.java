package com.ruralmarket.security;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.AuditLog;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.AuditLogRepository;
import com.ruralmarket.repo.SysUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示用无状态 Token 鉴权：登录后颁发 UUID，内存保存会话。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final Map<String, SysUser> SESSIONS = new ConcurrentHashMap<>();

    private final SysUserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AuthInterceptor(SysUserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (path.startsWith("/api/auth/login") || path.startsWith("/uploads/")
                || path.equals("/") || path.startsWith("/assets/") || path.startsWith("/index.html")
                || path.startsWith("/favicon") || path.startsWith("/api/health")) {
            return true;
        }
        if (!path.startsWith("/api/")) {
            return true;
        }
        String token = request.getHeader("X-Token");
        if (token == null) {
            token = request.getParameter("token");
        }
        SysUser cached = token == null ? null : SESSIONS.get(token);
        if (cached == null) {
            throw new ApiException(401, "未登录或登录已失效");
        }
        SysUser fresh = userRepository.findById(cached.getId()).orElseThrow(() -> new ApiException(401, "用户不存在"));
        CurrentUser.set(fresh);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SysUser u = CurrentUser.get();
        if (u != null && response.getStatus() >= 500) {
            AuditLog log = new AuditLog();
            log.setUsername(u.getUsername());
            log.setRole(u.getRole().name());
            log.setAction("HTTP " + response.getStatus());
            log.setDetail(request.getMethod() + " " + request.getRequestURI());
            log.setIp(request.getRemoteAddr());
            auditLogRepository.save(log);
        }
        CurrentUser.clear();
    }
}
