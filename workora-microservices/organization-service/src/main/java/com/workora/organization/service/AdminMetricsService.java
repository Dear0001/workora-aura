package com.workora.organization.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.organization.repository.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final CompanyRepository companyRepository;

    public AdminMetricsService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(companyRepository.count());
    }
}
