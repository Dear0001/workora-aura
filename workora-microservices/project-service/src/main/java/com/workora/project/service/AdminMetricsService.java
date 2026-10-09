package com.workora.project.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final ProjectRepository projectRepository;

    public AdminMetricsService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(projectRepository.count());
    }
}
