package com.workora.organization.mapper;

import com.workora.organization.dto.CompanyRatingResponse;
import com.workora.organization.dto.CompanyResponse;
import com.workora.organization.dto.DepartmentResponse;
import com.workora.organization.dto.DepartmentRoleAssignmentResponse;
import com.workora.organization.dto.InvitationResponse;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.entity.Company;
import com.workora.organization.entity.CompanyRating;
import com.workora.organization.entity.Department;
import com.workora.organization.entity.DepartmentRole;
import com.workora.organization.entity.Invitation;
import com.workora.organization.entity.Membership;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrganizationMapper {

    public CompanyResponse toResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getContactEmail(),
                company.getContactPhone(),
                company.getAddress(),
                company.getProfileImageUrl(),
                company.getCoverImageUrl(),
                company.getOwnerUserId(),
                company.getAverageRating(),
                company.getRatingCount(),
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }

    public CompanyRatingResponse toRatingResponse(CompanyRating rating) {
        return new CompanyRatingResponse(
                rating.getId(),
                rating.getCompanyId(),
                rating.getUserId(),
                rating.getScore(),
                rating.getReview(),
                rating.getCreatedAt()
        );
    }

    public DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getCompanyId(),
                department.getName(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt()
        );
    }

    public MemberResponse toResponse(Membership membership) {
        var departmentAssignments = membership.getDepartmentMemberships().stream()
                .map(assignment -> new DepartmentRoleAssignmentResponse(
                        assignment.getDepartmentId(), assignment.getRole()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (membership.getDepartmentId() != null
                && departmentAssignments.stream().noneMatch(
                assignment -> assignment.departmentId().equals(membership.getDepartmentId()))) {
            DepartmentRole legacyRole;
            try {
                legacyRole = DepartmentRole.valueOf(membership.getRole().name());
            } catch (IllegalArgumentException ignored) {
                legacyRole = DepartmentRole.DEVELOPER;
            }
            departmentAssignments.add(new DepartmentRoleAssignmentResponse(
                    membership.getDepartmentId(), legacyRole));
        }
        return new MemberResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getEmail(),
                membership.getRole(),
                membership.getDepartmentId(),
                List.copyOf(departmentAssignments),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }

    public InvitationResponse toResponse(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getCompanyId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getDepartmentId(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
