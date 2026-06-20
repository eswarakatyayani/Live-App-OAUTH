package com.katy.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public landing + health endpoints (no token required) so the service is
 * discoverable and Render's health check can succeed.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "mongo-oauth-demo");
        info.put("status", "UP");
        info.put("tokenEndpoint", "POST /oauth2/token (grant_type=client_credentials)");
        info.put("dataEndpoint", "GET /api/movies?limit=10 (requires Bearer token)");
        return info;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
