package com.workora.organization.service;

import com.workora.organization.dto.CompanyRatingRequest;
import com.workora.organization.dto.CompanyRatingResponse;
import com.workora.organization.entity.Company;
import com.workora.organization.entity.CompanyRating;
import com.workora.organization.exception.ConflictException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.CompanyRatingRepository;
import com.workora.organization.repository.CompanyRepository;
import com.workora.organization.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CompanyRatingService {

    private final CompanyRepository companyRepository;
    private final CompanyRatingRepository ratingRepository;
    private final MembershipService membershipService;
    private final OrganizationMapper mapper;

    public CompanyRatingService(CompanyRepository companyRepository,
                               CompanyRatingRepository ratingRepository,
                               MembershipService membershipService,
                               OrganizationMapper mapper) {
        this.companyRepository = companyRepository;
        this.ratingRepository = ratingRepository;
        this.membershipService = membershipService;
        this.mapper = mapper;
    }

    @Transactional
    public CompanyRatingResponse rateCompany(UUID companyId, CurrentUser caller, CompanyRatingRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        membershipService.requireActiveMembership(companyId, caller.userId());

        CompanyRating existing = ratingRepository.findByCompanyIdAndUserId(companyId, caller.userId()).orElse(null);
        if (existing != null && existing.getUserId().equals(caller.userId())) {
            throw new ConflictException("You have already rated this company");
        }

        CompanyRating rating = existing == null
                ? new CompanyRating(companyId, caller.userId(), request.score(), request.review())
                : existing;
        rating.setScore(request.score());
        rating.setReview(request.review());
        CompanyRating saved = ratingRepository.save(rating);

        refreshAggregate(company);
        return mapper.toRatingResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CompanyRatingResponse> list(UUID companyId, Pageable pageable) {
        return ratingRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(mapper::toRatingResponse);
    }

    private void refreshAggregate(Company company) {
        var ratings = ratingRepository.findByCompanyIdOrderByCreatedAtDesc(company.getId(), Pageable.unpaged());
        double average = ratings.getContent().stream().mapToDouble(CompanyRating::getScore).average().orElse(0.0);
        company.setAverageRating(Math.round(average * 100.0) / 100.0);
        company.setRatingCount(ratings.getTotalElements());
        companyRepository.save(company);
    }
}
