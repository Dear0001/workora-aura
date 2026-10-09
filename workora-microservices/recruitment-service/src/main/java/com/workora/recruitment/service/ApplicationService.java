package com.workora.recruitment.service;

import com.workora.recruitment.dto.ApplicationFileRequest;
import com.workora.recruitment.dto.ApplicationFileResponse;
import com.workora.recruitment.dto.ApplicationRequest;
import com.workora.recruitment.dto.ApplicationResponse;
import com.workora.recruitment.dto.ReviewerApplicationResponse;
import com.workora.recruitment.dto.StatusChangeRequest;
import com.workora.recruitment.entity.Application;
import com.workora.recruitment.entity.ApplicationFile;
import com.workora.recruitment.entity.ApplicationHistory;
import com.workora.recruitment.entity.ApplicationStatus;
import com.workora.recruitment.entity.PostStatus;
import com.workora.recruitment.entity.RecruitmentPost;
import com.workora.recruitment.exception.AppException;
import com.workora.recruitment.exception.ConflictException;
import com.workora.recruitment.exception.ForbiddenException;
import com.workora.recruitment.exception.NotFoundException;
import com.workora.recruitment.mapper.RecruitmentMapper;
import com.workora.recruitment.repository.ApplicationFileRepository;
import com.workora.recruitment.repository.ApplicationHistoryRepository;
import com.workora.recruitment.repository.ApplicationRepository;
import com.workora.recruitment.repository.RecruitmentPostRepository;
import com.workora.recruitment.security.CurrentUser;
import com.workora.common.storage.StorageKeyValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ApplicationService {

    private static final Set<ApplicationStatus> TERMINAL_STATUSES = EnumSet.of(
            ApplicationStatus.APPROVED_AND_MEMBER,
            ApplicationStatus.OWNER_REJECTED,
            ApplicationStatus.WITHDRAWN);

    private static final Set<ApplicationStatus> FILE_EDITABLE_STATUSES = EnumSet.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.MANAGER_REVIEW);

    private final ApplicationRepository applicationRepository;
    private final ApplicationFileRepository fileRepository;
    private final ApplicationHistoryRepository historyRepository;
    private final RecruitmentPostRepository postRepository;
    private final RecruitmentAuthorization authorization;
    private final RecruitmentMapper mapper;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationFileRepository fileRepository,
                              ApplicationHistoryRepository historyRepository,
                              RecruitmentPostRepository postRepository,
                              RecruitmentAuthorization authorization,
                              RecruitmentMapper mapper) {
        this.applicationRepository = applicationRepository;
        this.fileRepository = fileRepository;
        this.historyRepository = historyRepository;
        this.postRepository = postRepository;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    public ApplicationResponse apply(UUID postId, CurrentUser caller, ApplicationRequest request) {
        RecruitmentPost post = loadPost(postId);
        if (post.getStatus() != PostStatus.OPEN) {
            throw new AppException("Post is not accepting applications");
        }
        applicationRepository.findByPostIdAndApplicantId(postId, caller.userId())
                .ifPresent(existing -> {
                    throw new ConflictException("Already applied");
                });

        Application application = new Application();
        application.setPostId(postId);
        application.setApplicantId(caller.userId());
        application.setApplicantEmail(caller.email() == null ? "" : caller.email());
        application.setCoverNote(request.coverNote());
        application.setStructuredData(
                request.structuredData() == null ? new ArrayList<>() : new ArrayList<>(request.structuredData()));
        application.setStatus(ApplicationStatus.SUBMITTED);

        Application saved = applicationRepository.save(application);
        historyRepository.save(new ApplicationHistory(
                saved.getId(), null, ApplicationStatus.SUBMITTED, caller.userId(), null));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewerApplicationResponse> listForPost(UUID postId, CurrentUser caller) {
        RecruitmentPost post = loadPost(postId);
        authorization.requireCanReview(post, caller);
        return applicationRepository.findByPostId(postId).stream()
                .map(mapper::toReviewerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMine(CurrentUser caller) {
        return applicationRepository.findByApplicantId(caller.userId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Object get(UUID applicationId, CurrentUser caller) {
        Application application = loadApplication(applicationId);
        if (application.getApplicantId().equals(caller.userId())) {
            return mapper.toResponse(application);
        }
        RecruitmentPost post = loadPost(application.getPostId());
        try {
            authorization.requireCanReview(post, caller);
        } catch (ForbiddenException ex) {
            // Do not reveal existence of applications the caller cannot review.
            throw new NotFoundException("Application not found");
        }
        return mapper.toReviewerResponse(application);
    }

    public Object changeStatus(UUID applicationId, CurrentUser caller,
                               StatusChangeRequest request) {
        Application application = loadApplication(applicationId);
        RecruitmentPost post = loadPost(application.getPostId());
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.status();

        if (to == ApplicationStatus.WITHDRAWN) {
            authorizeWithdraw(application, from, caller);
        } else {
            authorizeTransition(post, from, to, caller, request.reason(), application);
        }

        application.setStatus(to);
        Application saved = applicationRepository.save(application);
        historyRepository.save(new ApplicationHistory(
                application.getId(), from, to, caller.userId(), request.reason()));
        boolean isApplicant = application.getApplicantId().equals(caller.userId());
        return isApplicant ? mapper.toResponse(saved) : mapper.toReviewerResponse(saved);
    }

    public ApplicationFileResponse addFile(UUID applicationId, CurrentUser caller,
                                           ApplicationFileRequest request) {
        Application application = loadApplication(applicationId);
        if (!application.getApplicantId().equals(caller.userId())) {
            throw new ForbiddenException(
                    "Only the applicant can attach files to this application", "FILE_UPLOAD_DENIED");
        }
        if (!FILE_EDITABLE_STATUSES.contains(application.getStatus())) {
            throw new AppException("Files can only be attached while the application is submitted or under manager review");
        }
        ApplicationFile file = new ApplicationFile();
        file.setApplicationId(applicationId);
        file.setFileKey(StorageKeyValidator.requireSafe(request.fileKey()));
        file.setMimeType(request.mimeType());
        return mapper.toResponse(fileRepository.save(file));
    }

    @Transactional(readOnly = true)
    public List<ApplicationFileResponse> listFiles(UUID applicationId, CurrentUser caller) {
        Application application = loadApplication(applicationId);
        boolean allowed = application.getApplicantId().equals(caller.userId());
        if (!allowed) {
            try {
                authorization.requireCanReview(loadPost(application.getPostId()), caller);
                allowed = true;
            } catch (ForbiddenException ignored) {
                // fall through to the uniform FILE_ACCESS_DENIED below
            }
        }
        if (!allowed) {
            throw new ForbiddenException(
                    "You cannot access the files of this application", "FILE_ACCESS_DENIED");
        }
        return fileRepository.findByApplicationId(applicationId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private void authorizeWithdraw(Application application, ApplicationStatus from, CurrentUser caller) {
        if (!application.getApplicantId().equals(caller.userId())) {
            throw new ForbiddenException(
                    "Only the applicant can withdraw this application", "WITHDRAW_DENIED");
        }
        if (TERMINAL_STATUSES.contains(from)) {
            throw new AppException("Cannot withdraw");
        }
    }

    private void authorizeTransition(RecruitmentPost post, ApplicationStatus from, ApplicationStatus to,
                                     CurrentUser caller, String reason, Application application) {
        switch (from) {
            case SUBMITTED -> {
                if (to != ApplicationStatus.MANAGER_REVIEW) {
                    throw invalidTransition();
                }
                authorization.requireCanReview(post, caller);
            }
            case MANAGER_REVIEW -> {
                if (to != ApplicationStatus.MANAGER_RECOMMENDED && to != ApplicationStatus.MANAGER_REJECTED) {
                    throw invalidTransition();
                }
                authorization.requireCanReview(post, caller);
                application.setManagerNote(reason);
            }
            case MANAGER_RECOMMENDED -> {
                if (to != ApplicationStatus.OWNER_APPROVAL) {
                    throw invalidTransition();
                }
                authorization.requireCanReview(post, caller);
            }
            case OWNER_APPROVAL -> {
                if (to != ApplicationStatus.APPROVED_AND_MEMBER && to != ApplicationStatus.OWNER_REJECTED) {
                    throw invalidTransition();
                }
                // Final approval only records the decision; the actual Organization
                // membership is created later by an event-driven pass.
                authorization.requireOwnerOrAdmin(post.getCompanyId(), caller);
                application.setOwnerNote(reason);
            }
            default -> throw invalidTransition();
        }
    }

    private AppException invalidTransition() {
        return new AppException("Invalid status transition");
    }

    private Application loadApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    private RecruitmentPost loadPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Recruitment post not found"));
    }
}
