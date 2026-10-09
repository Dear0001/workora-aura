package com.workora.organization.service;

import com.workora.organization.dto.ChangeRoleRequest;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.entity.CompanyPermission;
import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.CompanyStatus;
import com.workora.organization.entity.Department;
import com.workora.organization.entity.Membership;
import com.workora.organization.entity.MembershipAudit;
import com.workora.organization.entity.MembershipStatus;
import com.workora.organization.event.OrganizationEventPublisher;
import com.workora.organization.exception.ForbiddenException;
import com.workora.organization.exception.NotFoundException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.DepartmentRepository;
import com.workora.organization.repository.MembershipAuditRepository;
import com.workora.organization.repository.MembershipRepository;
import com.workora.organization.repository.CompanyRepository;
import com.workora.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final MembershipAuditRepository membershipAuditRepository;
    private final OrganizationEventPublisher organizationEventPublisher;
    private final OrganizationMapper mapper;

    public MembershipService(MembershipRepository membershipRepository,
                             DepartmentRepository departmentRepository,
                             OrganizationMapper mapper) {
        this(membershipRepository, departmentRepository, null, null, mapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public MembershipService(MembershipRepository membershipRepository,
                             DepartmentRepository departmentRepository,
                             MembershipAuditRepository membershipAuditRepository,
                             OrganizationEventPublisher organizationEventPublisher,
                             OrganizationMapper mapper,
                             CompanyRepository companyRepository) {
        this.membershipRepository = membershipRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.membershipAuditRepository = membershipAuditRepository;
        this.organizationEventPublisher = organizationEventPublisher;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Membership requireActiveMembership(UUID companyId, UUID userId) {
        if (companyRepository != null
                && companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE).isEmpty()) {
            throw new ForbiddenException("Company access is suspended or unavailable", "COMPANY_ACCESS_SUSPENDED");
        }
        return membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException(
                        "You are not a member of this company", "NOT_A_COMPANY_MEMBER"));
    }

    @Transactional(readOnly = true)
    public Membership requirePermission(UUID companyId, UUID userId, CompanyPermission permission) {
        Membership membership = requireActiveMembership(companyId, userId);
        if (!membership.hasPermission(permission) && !membership.getRole().isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "You do not have the required permission for this action",
                    "COMPANY_PERMISSION_REQUIRED");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public Membership requireOwnerOrAdmin(UUID companyId, UUID userId) {
        Membership membership = requireActiveMembership(companyId, userId);
        if (!membership.getRole().isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only a company owner or administrator can perform this action",
                    "COMPANY_ADMIN_REQUIRED");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID companyId, UUID actorId, String search) {
        requireActiveMembership(companyId, actorId);
        List<Membership> memberships = (search == null || search.isBlank())
                ? membershipRepository.findByCompanyIdAndStatusAndRoleNotOrderByCreatedAtAsc(
                        companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER)
                : membershipRepository.findByCompanyIdAndStatusAndRoleNotAndEmailContainingIgnoreCase(
                        companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER, search.trim());
        return memberships.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UUID companyId, UUID actorId) {
        return listMembers(companyId, actorId, null);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembersByDepartment(UUID departmentId) {
        return membershipRepository.findByDepartmentIdAndStatus(departmentId, MembershipStatus.ACTIVE)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getMyMembership(UUID companyId, UUID actorId) {
        return mapper.toResponse(requireActiveMembership(companyId, actorId));
    }

    @Transactional
    public MemberResponse changeRole(UUID companyId, UUID targetUserId, UUID actorId, ChangeRoleRequest request) {
        Membership actor = requireOwnerOrAdmin(companyId, actorId);
        Membership target = membershipRepository
                .findByCompanyIdAndUserIdAndStatus(companyId, targetUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Member not found in this company"));

        CompanyRole previousRole = target.getRole();
        boolean demotingOwner = target.getRole() == CompanyRole.OWNER && request.role() != CompanyRole.OWNER;
        if (demotingOwner && countOwners(companyId) <= 1) {
            throw new ForbiddenException(
                    "Cannot demote the final company owner; transfer ownership first",
                    "LAST_OWNER_PROTECTED");
        }
        boolean actorLosesAdmin = target.getUserId().equals(actor.getUserId())
                && actor.getRole().isOwnerOrAdmin()
                && !request.role().isOwnerOrAdmin();
        if (actorLosesAdmin && target.getRole() == CompanyRole.OWNER && countOwners(companyId) <= 1) {
            throw new ForbiddenException(
                    "Cannot remove your own owner role as the final owner",
                    "LAST_OWNER_PROTECTED");
        }

        if (request.departmentId() != null) {
            departmentRepository.findByIdAndCompanyId(request.departmentId(), companyId)
                    .orElseThrow(() -> new NotFoundException("Department not found in this company"));
        }

        target.setRole(request.role());
        target.setDepartmentId(request.departmentId());
        target.setPermissions(request.role().defaultPermissions());
        Membership saved = membershipRepository.save(target);
        if (membershipAuditRepository != null) {
            membershipAuditRepository.save(new MembershipAudit(
                    companyId,
                    targetUserId,
                    actorId,
                    "ROLE_CHANGED",
                    previousRole,
                    request.role(),
                    "Role changed to " + request.role().name()
            ));
        }
        if (organizationEventPublisher != null) {
            organizationEventPublisher.publish(
                    "membership.role_changed",
                    companyId,
                    actorId,
                    targetUserId,
                    Map.of("previousRole", previousRole.name(), "newRole", request.role().name())
            );
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public void removeMember(UUID companyId, UUID targetUserId, UUID actorId) {
        requireOwnerOrAdmin(companyId, actorId);
        Membership target = membershipRepository
                .findByCompanyIdAndUserIdAndStatus(companyId, targetUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Member not found in this company"));

        if (targetUserId.equals(actorId)) {
            throw new ForbiddenException(
                    "You cannot remove yourself from the company",
                    "SELF_REMOVAL_NOT_ALLOWED");
        }

        if (target.getRole() == CompanyRole.OWNER && countOwners(companyId) <= 1) {
            throw new ForbiddenException(
                    "Cannot remove the final company owner", "LAST_OWNER_PROTECTED");
        }

        CompanyRole previousRole = target.getRole();
        target.setStatus(MembershipStatus.REMOVED);
        target.setRemovedAt(Instant.now());
        membershipRepository.save(target);
        if (membershipAuditRepository != null) {
            membershipAuditRepository.save(new MembershipAudit(
                    companyId,
                    targetUserId,
                    actorId,
                    "MEMBER_REMOVED",
                    previousRole,
                    null,
                    "Member removed from company"
            ));
        }
        if (organizationEventPublisher != null) {
            organizationEventPublisher.publish(
                    "membership.removed",
                    companyId,
                    actorId,
                    targetUserId,
                    Map.of("role", previousRole.name())
            );
        }
    }

    @Transactional
    public Membership createMembership(UUID companyId, UUID userId, String email, CompanyRole role,
                                       UUID departmentId, UUID invitedBy) {
        Membership membership = new Membership(companyId, userId, email, role, departmentId, invitedBy);
        membership.setPermissions(role.defaultPermissions());
        Membership saved = membershipRepository.save(membership);
        if (membershipAuditRepository != null) {
            membershipAuditRepository.save(new MembershipAudit(
                    companyId,
                    userId,
                    invitedBy == null ? userId : invitedBy,
                    "MEMBER_ADDED",
                    null,
                    role,
                    "Membership created"
            ));
        }
        if (organizationEventPublisher != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("role", role.name());
            if (departmentId != null) {
                payload.put("departmentId", departmentId);
            }
            organizationEventPublisher.publish(
                    "membership.created",
                    companyId,
                    invitedBy == null ? userId : invitedBy,
                    userId,
                    payload
            );
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MembershipAudit> auditFor(UUID companyId, UUID targetUserId, UUID actorId) {
        requireActiveMembership(companyId, actorId);
        return membershipAuditRepository.findByCompanyIdAndUserIdOrderByCreatedAtDesc(companyId, targetUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MembershipAudit> adminActivity(
            UUID companyId, UUID userId, String action, Instant from, Instant to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activity start time must be before end time");
        }
        var events = membershipAuditRepository.searchAdminActivity(
                companyId,
                userId,
                action == null || action.isBlank() ? null : action.trim(),
                from,
                to,
                pageable);
        return PageResponse.of(events.getContent(), events.getNumber(), events.getSize(), events.getTotalElements());
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(UUID companyId, UUID userId) {
        return membershipRepository.existsByCompanyIdAndUserIdAndStatus(companyId, userId, MembershipStatus.ACTIVE);
    }

    @Transactional
    public MemberResponse assignToDepartment(UUID departmentId, UUID targetUserId, UUID actorId) {
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> d.getStatus() == CompanyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        requireOwnerOrAdmin(department.getCompanyId(), actorId);
        Membership target = membershipRepository
                .findByCompanyIdAndUserIdAndStatus(department.getCompanyId(), targetUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Member not found in this company"));
        target.setDepartmentId(departmentId);
        return mapper.toResponse(membershipRepository.save(target));
    }

    @Transactional
    public void removeFromDepartment(UUID departmentId, UUID targetUserId, UUID actorId) {
        Department department = departmentRepository.findById(departmentId)
                .filter(d -> d.getStatus() == CompanyStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Department not found"));
        requireOwnerOrAdmin(department.getCompanyId(), actorId);
        Membership target = membershipRepository
                .findByCompanyIdAndUserIdAndStatus(department.getCompanyId(), targetUserId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Member not found in this company"));
        if (!departmentId.equals(target.getDepartmentId())) {
            throw new NotFoundException("Member is not assigned to this department");
        }
        target.setDepartmentId(null);
        membershipRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<Membership> activeMembershipsFor(UUID userId) {
        return membershipRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, MembershipStatus.ACTIVE);
    }

    private long countOwners(UUID companyId) {
        return membershipRepository.countByCompanyIdAndRoleAndStatus(companyId, CompanyRole.OWNER, MembershipStatus.ACTIVE);
    }
}
