package com.workora.organization.service;

import com.workora.organization.dto.SubscriptionResponse;
import com.workora.organization.dto.AdminSubscriptionResponse;
import com.workora.organization.dto.AdminSubscriptionUpdateRequest;
import com.workora.organization.entity.Subscription;
import com.workora.common.dto.PageResponse;
import com.workora.organization.exception.NotFoundException;
import com.workora.organization.repository.SubscriptionRepository;
import com.workora.organization.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public Subscription getOrCreate(CurrentUser caller) {
        return subscriptionRepository.findByUserId(caller.userId())
                .orElseGet(() -> subscriptionRepository.save(new Subscription(caller.userId())));
    }

    @Transactional
    public SubscriptionResponse me(CurrentUser caller) {
        return SubscriptionResponse.from(getOrCreate(caller));
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminSubscriptionResponse> adminList(Pageable pageable) {
        var subscriptions = subscriptionRepository.findAllByOrderByUpdatedAtDesc(pageable);
        return PageResponse.of(subscriptions.getContent().stream()
                        .map(AdminSubscriptionResponse::from).toList(),
                subscriptions.getNumber(), subscriptions.getSize(), subscriptions.getTotalElements());
    }

    @Transactional
    public AdminSubscriptionResponse adminUpdate(UUID userId, AdminSubscriptionUpdateRequest request) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Subscription not found for this account"));
        subscription.setPlan(request.plan());
        subscription.setMaxCompanies(request.maxCompanies());
        subscription.setEntitled(request.entitled());
        return AdminSubscriptionResponse.from(subscriptionRepository.save(subscription));
    }
}
