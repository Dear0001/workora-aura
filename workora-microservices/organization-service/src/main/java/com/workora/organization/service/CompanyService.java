package com.workora.organization.service;

import com.workora.common.dto.PageResponse;
import com.workora.organization.dto.CompanyRequest;
import com.workora.organization.dto.CompanyResponse;
import com.workora.organization.dto.CompanyUpdateRequest;
import com.workora.organization.entity.Company;
import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.CompanyStatus;
import com.workora.organization.exception.NotFoundException;
import com.workora.organization.exception.ForbiddenException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.CompanyRepository;
import com.workora.organization.security.CurrentUser;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final MembershipService membershipService;
    private final OrganizationMapper mapper;
    private final SubscriptionService subscriptionService;

    public CompanyService(CompanyRepository companyRepository,
                          MembershipService membershipService,
                          OrganizationMapper mapper,
                          SubscriptionService subscriptionService) {
        this.companyRepository = companyRepository;
        this.membershipService = membershipService;
        this.mapper = mapper;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public CompanyResponse create(CurrentUser caller, CompanyRequest request) {
        var subscription = subscriptionService.getOrCreate(caller);
        long ownedActiveCompanies = companyRepository.countByOwnerUserIdAndStatus(caller.userId(), CompanyStatus.ACTIVE);
        if (ownedActiveCompanies >= subscription.getMaxCompanies()) {
            throw new ForbiddenException("Your subscription does not allow another active company", "COMPANY_QUOTA_EXCEEDED");
        }
        Company company = new Company(request.name().trim(), caller.userId());
        company.setDescription(request.description());
        company.setContactEmail(request.contactEmail());
        company.setContactPhone(request.contactPhone());
        company.setAddress(request.address());
        company.setProfileImageUrl(request.profileImageUrl());
        company.setCoverImageUrl(request.coverImageUrl());
        Company saved = companyRepository.save(company);

        membershipService.createMembership(
                saved.getId(),
                caller.userId(),
                caller.email(),
                CompanyRole.OWNER,
                null,
                caller.userId()
        );
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyResponse get(UUID companyId, CurrentUser caller) {
        membershipService.requireActiveMembership(companyId, caller.userId());
        return mapper.toResponse(loadActive(companyId));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> listMine(CurrentUser caller, Pageable pageable) {
        return listMine(caller, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> listMine(CurrentUser caller, String search, Pageable pageable) {
        List<UUID> companyIds = membershipService.activeMembershipsFor(caller.userId())
                .stream()
                .map(membership -> membership.getCompanyId())
                .distinct()
                .toList();
        if (companyIds.isEmpty()) {
            return PageResponse.of(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0);
        }
        List<Company> filtered = companyRepository.findAllById(companyIds).stream()
                .filter(company -> company.getStatus() == CompanyStatus.ACTIVE)
                .filter(company -> search == null || search.isBlank() || company.getName().toLowerCase().contains(search.toLowerCase()))
                .sorted(Comparator.comparing(Company::getCreatedAt).reversed())
                .toList();
        int from = Math.min(pageable.getPageNumber() * pageable.getPageSize(), filtered.size());
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        List<CompanyResponse> content = filtered.subList(from, to).stream().map(mapper::toResponse).toList();
        return PageResponse.of(content, pageable.getPageNumber(), pageable.getPageSize(), filtered.size());
    }

    @Transactional
    public CompanyResponse update(UUID companyId, CurrentUser caller, CompanyUpdateRequest request) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        Company company = loadActive(companyId);
        if (request.name() != null && !request.name().isBlank()) {
            company.setName(request.name().trim());
        }
        if (request.description() != null) {
            company.setDescription(request.description());
        }
        if (request.contactEmail() != null) {
            company.setContactEmail(request.contactEmail());
        }
        if (request.contactPhone() != null) {
            company.setContactPhone(request.contactPhone());
        }
        if (request.address() != null) {
            company.setAddress(request.address());
        }
        if (request.profileImageUrl() != null) {
            company.setProfileImageUrl(request.profileImageUrl());
        }
        if (request.coverImageUrl() != null) {
            company.setCoverImageUrl(request.coverImageUrl());
        }
        return mapper.toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse archive(UUID companyId, CurrentUser caller) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        Company company = loadActive(companyId);
        company.setStatus(CompanyStatus.ARCHIVED);
        company.setArchivedAt(Instant.now());
        return mapper.toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public PageResponse<CompanyResponse> adminList(org.springframework.data.domain.Pageable pageable) {
        var companies = companyRepository.findAll(pageable);
        return PageResponse.of(companies.getContent().stream().map(mapper::toResponse).toList(),
                companies.getNumber(), companies.getSize(), companies.getTotalElements());
    }

    @Transactional
    public CompanyResponse adminSetStatus(UUID companyId, CompanyStatus status) {
        if (status != CompanyStatus.ACTIVE && status != CompanyStatus.SUSPENDED) {
            throw new ForbiddenException("Admin status must be ACTIVE or SUSPENDED", "INVALID_COMPANY_STATUS");
        }
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
        if (company.getStatus() == CompanyStatus.ARCHIVED) {
            throw new ForbiddenException("Archived companies cannot be reactivated", "COMPANY_ARCHIVED");
        }
        company.setStatus(status);
        return mapper.toResponse(companyRepository.save(company));
    }

    @Transactional(readOnly = true)
    public Company loadActive(UUID companyId) {
        return companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    @Transactional(readOnly = true)
    public String loadName(UUID companyId) {
        return companyRepository.findById(companyId)
                .map(Company::getName)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }
}
