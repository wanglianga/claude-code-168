package com.ruralmarket.platform.controller;

import com.ruralmarket.platform.config.AuthUser;
import com.ruralmarket.platform.config.BizException;
import com.ruralmarket.platform.config.JwtService;
import com.ruralmarket.platform.entity.User;
import com.ruralmarket.platform.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}
    public record RegisterReq(@NotBlank String username, @NotBlank String password,
                              @NotBlank String name, String phone) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginReq req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        return tokenPayload(user);
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterReq req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new BizException("用户名已存在");
        }
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(passwordEncoder.encode(req.password()));
        u.setRole("VENDOR");
        u.setName(req.name());
        u.setPhone(req.phone());
        userRepository.save(u);
        return tokenPayload(u);
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AuthUser au = AuthUser.current();
        User user = userRepository.findById(au.id()).orElseThrow();
        return userPayload(user, null);
    }

    private Map<String, Object> tokenPayload(User user) {
        String token = jwtService.generate(user.getId(), user.getUsername(), user.getRole(), user.getName());
        return userPayload(user, token);
    }

    private Map<String, Object> userPayload(User user, String token) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (token != null) map.put("token", token);
        map.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "name", user.getName(),
                "role", user.getRole(),
                "creditScore", user.getCreditScore(),
                "historyCount", user.getHistoryCount()));
        return map;
    }
}
