package com.workora.work.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.work.repository.TaskRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final TaskRepository taskRepository;

    public AdminMetricsService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(taskRepository.count());
    }
}
