package com.workora.notification.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final NotificationRepository notificationRepository;

    public AdminMetricsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(notificationRepository.count());
    }
}
