package com.workora.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GatewayController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "success", true,
                "message", "Workora API Gateway is running"
        );
    }
}
