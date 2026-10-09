package com.workora.dashboard.dto;

import java.time.Instant;
import java.util.Map;

public record AdminServiceHealthResponse(
        String service,
        String status,
        Instant checkedAt,
        Map<String, String> components) {
}
