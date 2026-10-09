package com.workora.identity.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final UserRepository userRepository;

    public AdminMetricsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(userRepository.count());
    }
}
