package com.workora.bug.service;

import com.workora.bug.repository.BugReportRepository;
import com.workora.common.dto.AdminMetricResponse;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final BugReportRepository bugReportRepository;

    public AdminMetricsService(BugReportRepository bugReportRepository) {
        this.bugReportRepository = bugReportRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(bugReportRepository.count());
    }
}
