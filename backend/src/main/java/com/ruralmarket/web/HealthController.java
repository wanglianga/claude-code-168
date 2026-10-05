package com.ruralmarket.web;

import com.ruralmarket.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        return R.ok(Map.of("status", "UP", "service", "market-fair-platform"));
    }
}
