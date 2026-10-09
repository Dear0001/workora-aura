package com.workora.organization.mapper;

import com.workora.organization.dto.CompanyRatingResponse;
import com.workora.organization.dto.CompanyResponse;
import com.workora.organization.dto.DepartmentResponse;
import com.workora.organization.dto.InvitationResponse;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.entity.Company;
import com.workora.organization.entity.CompanyRating;
import com.workora.organization.entity.Department;
import com.workora.organization.entity.Invitation;
import com.workora.organization.entity.Membership;
import org.springframework.stereotype.Component;

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
        return new MemberResponse(
                membership.getId(),
                membership.getUserId(),
                membership.getEmail(),
                membership.getRole(),
                membership.getDepartmentId(),
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
