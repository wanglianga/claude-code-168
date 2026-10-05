package com.ruralmarket.web;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.common.R;
import com.ruralmarket.domain.SysUser;
import com.ruralmarket.repo.SysUserRepository;
import com.ruralmarket.security.AuthInterceptor;
import com.ruralmarket.security.CurrentUser;
import com.ruralmarket.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepository;
    private final AuditService auditService;

    public AuthController(SysUserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req.username == null || req.password == null) {
            throw new ApiException("请输入用户名和密码");
        }
        SysUser user = userRepository.findByUsername(req.username.trim())
                .orElseThrow(() -> new ApiException(401, "用户名或密码错误"));
        if (!user.getPassword().equals(req.password)) {
            throw new ApiException(401, "用户名或密码错误");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        AuthInterceptor.SESSIONS.put(token, user);
        auditService.log(user, "登录", user.getRole().getLabel() + "登录", request.getRemoteAddr());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("user", user);
        return R.ok(result);
    }

    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        if (token != null) {
            AuthInterceptor.SESSIONS.remove(token);
        }
        return R.ok();
    }

    @GetMapping("/me")
    public R<SysUser> me() {
        return R.ok(CurrentUser.require());
    }
}
