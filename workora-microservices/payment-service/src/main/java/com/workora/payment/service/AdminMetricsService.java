package com.workora.payment.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMetricsService {
    private final PaymentRepository paymentRepository;

    public AdminMetricsService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public AdminMetricResponse metrics() {
        return new AdminMetricResponse(paymentRepository.count());
    }
}
