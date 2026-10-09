package com.workora.recruitment.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.recruitment.repository.RecruitmentPostRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final RecruitmentPostRepository recruitmentPostRepository;

    public AdminMetricsService(RecruitmentPostRepository recruitmentPostRepository) {
        this.recruitmentPostRepository = recruitmentPostRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(recruitmentPostRepository.count());
    }
}
