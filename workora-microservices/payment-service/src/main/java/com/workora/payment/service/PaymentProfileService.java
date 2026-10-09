package com.workora.payment.service;

import com.workora.payment.dto.*;
import com.workora.payment.entity.PaymentProfile;
import com.workora.payment.repository.PaymentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@Transactional
public class PaymentProfileService {
    private static final String BAKONG = "BAKONG";
    private final PaymentProfileRepository repository;
    public PaymentProfileService(PaymentProfileRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public PaymentProfileResponse get(UUID ownerId) {
        return repository.findByOwnerIdAndProvider(ownerId, BAKONG).map(this::response).orElse(null);
    }
    public PaymentProfileResponse create(UUID ownerId, PaymentProfileRequest request) {
        PaymentProfile profile = repository.findByOwnerIdAndProvider(ownerId, BAKONG)
                .orElseGet(() -> new PaymentProfile(ownerId, BAKONG, request.walletName().trim(),
                        request.phoneNumber().trim(), request.isPrimary()));
        apply(profile, request);
        return response(repository.save(profile));
    }
    public PaymentProfileResponse update(UUID ownerId, PaymentProfileRequest request) {
        PaymentProfile profile = repository.findByOwnerIdAndProvider(ownerId, BAKONG)
                .orElseThrow(() -> new IllegalArgumentException("Bakong payment profile not found"));
        apply(profile, request);
        return response(repository.save(profile));
    }
    public void delete(UUID ownerId) {
        repository.findByOwnerIdAndProvider(ownerId, BAKONG).ifPresent(repository::delete);
    }
    public PaymentProfileResponse uploadQr(UUID ownerId, String payload) {
        PaymentProfile profile = repository.findByOwnerIdAndProvider(ownerId, BAKONG)
                .orElseThrow(() -> new IllegalArgumentException("Bakong payment profile not found"));
        profile.setQrPayload(payload);
        return response(repository.save(profile));
    }
    private void apply(PaymentProfile p, PaymentProfileRequest r) {
        p.setWalletName(r.walletName().trim()); p.setPhoneNumber(r.phoneNumber().trim());
        p.setPrimaryProfile(r.isPrimary());
    }
    private PaymentProfileResponse response(PaymentProfile p) {
        return new PaymentProfileResponse(p.getId(), p.getProvider(), p.getWalletName(), p.getPhoneNumber(),
                p.isPrimaryProfile(), p.getQrPayload(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
