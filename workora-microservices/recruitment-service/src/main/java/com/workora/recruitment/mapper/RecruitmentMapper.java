package com.workora.recruitment.mapper;

import com.workora.recruitment.dto.ApplicationFileResponse;
import com.workora.recruitment.dto.ApplicationResponse;
import com.workora.recruitment.dto.PublicPostResponse;
import com.workora.recruitment.dto.RecruitmentPostResponse;
import com.workora.recruitment.dto.ReviewerApplicationResponse;
import com.workora.recruitment.entity.Application;
import com.workora.recruitment.entity.ApplicationFile;
import com.workora.recruitment.entity.RecruitmentPost;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecruitmentMapper {

    public RecruitmentPostResponse toResponse(RecruitmentPost post) {
        return new RecruitmentPostResponse(
                post.getId(),
                post.getCompanyId(),
                post.getDepartmentId(),
                post.getProjectId(),
                post.getTitle(),
                post.getDescription(),
                post.getFee(),
                post.getCurrency(),
                post.getImageUrl(),
                post.getIntendedRole(),
                post.getAuthorId(),
                post.getStatus(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getClosedAt());
    }

    public PublicPostResponse toPublicResponse(RecruitmentPost post) {
        return new PublicPostResponse(
                post.getId(),
                post.getTitle(),
                post.getDescription(),
                post.getFee(),
                post.getCurrency(),
                post.getIntendedRole(),
                post.getStatus(),
                post.getCreatedAt());
    }

    public ApplicationResponse toResponse(Application application) {
        return new ApplicationResponse(
                application.getId(),
                application.getPostId(),
                application.getApplicantId(),
                application.getApplicantEmail(),
                application.getCoverNote(),
                List.copyOf(application.getStructuredData()),
                application.getStatus(),
                application.getSubmittedAt(),
                application.getUpdatedAt());
    }

    public ReviewerApplicationResponse toReviewerResponse(Application application) {
        return new ReviewerApplicationResponse(
                application.getId(),
                application.getPostId(),
                application.getApplicantId(),
                application.getApplicantEmail(),
                application.getCoverNote(),
                List.copyOf(application.getStructuredData()),
                application.getStatus(),
                application.getManagerNote(),
                application.getOwnerNote(),
                application.getSubmittedAt(),
                application.getUpdatedAt());
    }

    public ApplicationFileResponse toResponse(ApplicationFile file) {
        return new ApplicationFileResponse(
                file.getId(),
                file.getApplicationId(),
                file.getFileKey(),
                file.getMimeType(),
                file.getUploadedAt());
    }
}
