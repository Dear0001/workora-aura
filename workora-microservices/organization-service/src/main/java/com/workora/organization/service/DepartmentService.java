package com.workora.organization.service;

import com.workora.organization.dto.DepartmentRequest;
import com.workora.organization.dto.DepartmentResponse;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.entity.CompanyStatus;
import com.workora.organization.entity.Department;
import com.workora.organization.exception.ConflictException;
import com.workora.organization.exception.NotFoundException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.DepartmentRepository;
import com.workora.organization.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CompanyService companyService;
    private final MembershipService membershipService;
    private final OrganizationMapper mapper;

    public DepartmentService(DepartmentRepository departmentRepository,
                             CompanyService companyService,
                             MembershipService membershipService,
                             OrganizationMapper mapper) {
        this.departmentRepository = departmentRepository;
        this.companyService = companyService;
        this.membershipService = membershipService;
        this.mapper = mapper;
    }

    @Transactional
    public DepartmentResponse create(UUID companyId, CurrentUser caller, DepartmentRequest request) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        companyService.loadActive(companyId);
        String name = request.name().trim();
        if (departmentRepository.existsByCompanyIdAndNameAndStatus(companyId, name, CompanyStatus.ACTIVE)) {
            throw new ConflictException("A department with this name already exists in the company");
        }
        Department department = new Department(companyId, name);
        department.setDescription(request.description());
        return mapper.toResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(UUID companyId, CurrentUser caller) {
        return list(companyId, caller, null);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(UUID companyId, CurrentUser caller, String search) {
        membershipService.requireActiveMembership(companyId, caller.userId());
        List<Department> departments = search == null || search.isBlank()
                ? departmentRepository.findByCompanyIdAndStatusOrderByCreatedAtAsc(companyId, CompanyStatus.ACTIVE)
                : departmentRepository.findByCompanyIdAndStatusAndNameContainingIgnoreCase(companyId, CompanyStatus.ACTIVE, search.trim());
        return departments.stream().map(mapper::toResponse).toList();
    }

    /**
     * Resolves a department by id for a caller who is a member of the owning company.
     * Used by the caller's own membership/role checks and by other services that need
     * to map a department to its company.
     */
    @Transactional(readOnly = true)
    public DepartmentResponse getForCaller(UUID departmentId, CurrentUser caller) {
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> d.getStatus() == CompanyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        membershipService.requireActiveMembership(department.getCompanyId(), caller.userId());
        return mapper.toResponse(department);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID departmentId, CurrentUser caller) {
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> d.getStatus() == CompanyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        membershipService.requireActiveMembership(department.getCompanyId(), caller.userId());
        return membershipService.listMembersByDepartment(departmentId);
    }

    @Transactional
    public DepartmentResponse update(UUID companyId, UUID departmentId, CurrentUser caller, DepartmentRequest request) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        Department department = load(companyId, departmentId);
        String name = request.name().trim();
        if (!name.equals(department.getName())
                && departmentRepository.existsByCompanyIdAndNameAndStatus(companyId, name, CompanyStatus.ACTIVE)) {
            throw new ConflictException("A department with this name already exists in the company");
        }
        department.setName(name);
        department.setDescription(request.description());
        return mapper.toResponse(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponse archive(UUID companyId, UUID departmentId, CurrentUser caller) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        Department department = load(companyId, departmentId);
        department.setStatus(CompanyStatus.ARCHIVED);
        department.setArchivedAt(Instant.now());
        return mapper.toResponse(departmentRepository.save(department));
    }

    @Transactional(readOnly = true)
    public Department load(UUID companyId, UUID departmentId) {
        Department department = departmentRepository.findByIdAndCompanyId(departmentId, companyId)
                .orElseThrow(() -> new NotFoundException("Department not found in this company"));
        if (department.getStatus() != CompanyStatus.ACTIVE) {
            throw new NotFoundException("Department not found in this company");
        }
        return department;
    }
}
