package com.workora.payment.service;

import com.workora.common.dto.PageResponse;
import com.workora.payment.dto.*;
import com.workora.payment.entity.*;
import com.workora.payment.repository.PaymentRepository;
import com.workora.payment.exception.PaymentNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {
    private final PaymentRepository repository;
    private final PaymentProvider provider;
    private final PaymentEventPublisher events;
    public PaymentService(PaymentRepository repository, PaymentProvider provider, PaymentEventPublisher events) {
        this.repository = repository; this.provider = provider; this.events = events;
    }
    public PaymentResponse create(UUID payerId, PaymentCreateRequest request) {
        String reference = request.idempotencyKey();
        if (reference != null && !reference.isBlank()) {
            var existing = repository.findByPayerIdAndReference(payerId, reference);
            if (existing.isPresent()) return response(existing.get());
        } else {
            reference = "WO-" + UUID.randomUUID();
        }
        Instant expiry = Instant.now().plus(15, ChronoUnit.MINUTES);
        PaymentProvider.PaymentCreation creation = provider.createTransaction(reference, request.amount(),
                request.currency().toUpperCase(), request.purpose(), null);
        Payment payment = new Payment(payerId, reference, request.amount(),
                request.currency().toUpperCase(), request.purpose(), provider.name(),
                creation.qrCode(), expiry);
        payment.setProviderReference(creation.providerReference());
        payment.setDeepLink(creation.deepLink());
        return response(repository.save(payment));
    }
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(UUID payerId, Pageable pageable) {
        Page<Payment> page = repository.findByPayerIdOrderByCreatedAtDesc(payerId, pageable);
        return PageResponse.of(page.getContent().stream().map(this::response).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
    @Transactional(readOnly = true)
    public PaymentResponse get(UUID payerId, UUID id) {
        return response(repository.findById(id).filter(p -> p.getPayerId().equals(payerId))
                .orElseThrow(PaymentNotFoundException::new));
    }
    public PaymentResponse verify(UUID payerId, UUID id, PaymentVerifyRequest request) {
        Payment payment = repository.findById(id).filter(p -> p.getPayerId().equals(payerId))
                .orElseThrow(PaymentNotFoundException::new);
        if (payment.getStatus() == PaymentStatus.PAID) return response(payment);
        if (payment.getExpiresAt().isBefore(Instant.now())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            return response(payment);
        }
        if (!provider.verify(payment.getReference(), request.providerReference(),
                payment.getAmount(), payment.getCurrency())) {
            throw new IllegalArgumentException("Payment could not be verified");
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderReference(request.providerReference());
        payment.setPaidAt(Instant.now());
        Payment saved = repository.save(payment);
        events.paid(saved);
        return response(saved);
    }
    public PaymentResponse refreshStatus(UUID payerId, UUID id) {
        Payment payment = owned(payerId, id);
        if (payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.RECEIPT_CONFIRMED) {
            return response(payment);
        }
        if (payment.getExpiresAt().isBefore(Instant.now())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            return response(repository.save(payment));
        }
        PaymentProvider.PaymentStatusResult result = provider.transactionStatus(payment.getReference(),
                payment.getProviderReference(), payment.getAmount(), payment.getCurrency());
        if (!result.successful()) return response(payment);
        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderReference(result.providerReference());
        payment.setPaidAt(Instant.now());
        Payment saved = repository.save(payment);
        events.paid(saved);
        return response(saved);
    }
    public PaymentResponse handleCallback(String reference, String providerReference, boolean successful) {
        Payment payment = repository.findByReference(reference)
                .orElseThrow(PaymentNotFoundException::new);
        if (!successful || payment.getStatus() == PaymentStatus.PAID
                || payment.getStatus() == PaymentStatus.RECEIPT_CONFIRMED) {
            return response(payment);
        }
        payment.setStatus(PaymentStatus.PAID);
        payment.setProviderReference(providerReference);
        payment.setPaidAt(Instant.now());
        Payment saved = repository.save(payment);
        events.paid(saved);
        return response(saved);
    }
    public PaymentResponse confirmReceipt(UUID payerId, UUID id) {
        Payment payment = owned(payerId, id);
        if (payment.getStatus() == PaymentStatus.RECEIPT_CONFIRMED) return response(payment);
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalArgumentException("Receipt can only be confirmed after payment verification");
        }
        payment.setStatus(PaymentStatus.RECEIPT_CONFIRMED);
        payment.setReceiptConfirmedAt(Instant.now());
        return response(repository.save(payment));
    }
    public PaymentResponse dispute(UUID payerId, UUID id, String reason) {
        Payment payment = owned(payerId, id);
        if (payment.getStatus() == PaymentStatus.DISPUTED) return response(payment);
        if (payment.getStatus() != PaymentStatus.PAID
                && payment.getStatus() != PaymentStatus.RECEIPT_CONFIRMED) {
            throw new IllegalArgumentException("Only verified payments can be disputed");
        }
        payment.setStatus(PaymentStatus.DISPUTED);
        payment.setDisputeReason(reason);
        return response(repository.save(payment));
    }
    private Payment owned(UUID payerId, UUID id) {
        return repository.findById(id).filter(p -> p.getPayerId().equals(payerId))
                .orElseThrow(PaymentNotFoundException::new);
    }
    private PaymentResponse response(Payment p) {
        return new PaymentResponse(p.getId(), p.getReference(), p.getAmount(), p.getCurrency(),
                p.getPurpose(), p.getStatus(), p.getProvider(), p.getQrPayload(), p.getDeepLink(),
                p.getExpiresAt(), p.getPaidAt(), p.getCreatedAt(),
                p.getReceiptConfirmedAt(), p.getDisputeReason());
    }
}
