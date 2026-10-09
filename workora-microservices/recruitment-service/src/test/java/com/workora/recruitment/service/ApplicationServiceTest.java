package com.workora.recruitment.service;

import com.workora.recruitment.client.CallerMembership;
import com.workora.recruitment.client.OrganizationClient;
import com.workora.recruitment.dto.ApplicationRequest;
import com.workora.recruitment.dto.StatusChangeRequest;
import com.workora.recruitment.entity.Application;
import com.workora.recruitment.entity.ApplicationStatus;
import com.workora.recruitment.entity.PostStatus;
import com.workora.recruitment.entity.RecruitmentPost;
import com.workora.recruitment.exception.AppException;
import com.workora.recruitment.exception.ConflictException;
import com.workora.recruitment.exception.ForbiddenException;
import com.workora.recruitment.mapper.RecruitmentMapper;
import com.workora.recruitment.repository.ApplicationFileRepository;
import com.workora.recruitment.repository.ApplicationHistoryRepository;
import com.workora.recruitment.repository.ApplicationRepository;
import com.workora.recruitment.repository.RecruitmentPostRepository;
import com.workora.recruitment.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    private static final UUID POST_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID DEPARTMENT_ID = UUID.randomUUID();
    private static final UUID APPLICANT_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID DEVELOPER_ID = UUID.randomUUID();

    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationFileRepository fileRepository;
    @Mock
    private ApplicationHistoryRepository historyRepository;
    @Mock
    private RecruitmentPostRepository postRepository;
    @Mock
    private OrganizationClient organizationClient;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        RecruitmentAuthorization authorization =
                new RecruitmentAuthorization(organizationClient);
        service = new ApplicationService(applicationRepository, fileRepository,
                historyRepository, postRepository, authorization, new RecruitmentMapper());
    }

    private RecruitmentPost post(PostStatus status) {
        RecruitmentPost post = new RecruitmentPost();
        post.setId(POST_ID);
        post.setCompanyId(COMPANY_ID);
        post.setDepartmentId(DEPARTMENT_ID);
        post.setTitle("Designer needed");
        post.setDescription("Design a landing page");
        post.setAuthorId(OWNER_ID);
        post.setStatus(status);
        return post;
    }

    private Application application(ApplicationStatus status) {
        Application application = new Application();
        application.setId(UUID.randomUUID());
        application.setPostId(POST_ID);
        application.setApplicantId(APPLICANT_ID);
        application.setApplicantEmail("applicant@example.com");
        application.setStatus(status);
        application.setStructuredData(List.of("portfolio=https://example.com"));
        return application;
    }

    @Test
    void applyRejectedWhenPostIsNotOpen() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostStatus.DRAFT)));

        AppException ex = assertThrows(AppException.class, () -> service.apply(POST_ID,
                new CurrentUser(APPLICANT_ID, "applicant@example.com"),
                new ApplicationRequest("I am interested", List.of())));

        assertEquals("Post is not accepting applications", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void duplicateApplyRejectedWithConflict() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostStatus.OPEN)));
        when(applicationRepository.findByPostIdAndApplicantId(POST_ID, APPLICANT_ID))
                .thenReturn(Optional.of(application(ApplicationStatus.SUBMITTED)));

        ConflictException ex = assertThrows(ConflictException.class, () -> service.apply(POST_ID,
                new CurrentUser(APPLICANT_ID, "applicant@example.com"),
                new ApplicationRequest("Again", List.of())));

        assertEquals("Already applied", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void ownerApprovalAllowedForOwner() {
        Application application = application(ApplicationStatus.OWNER_APPROVAL);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostStatus.OPEN)));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(organizationClient.myMembership(COMPANY_ID))
                .thenReturn(new CallerMembership("OWNER", null));
        when(applicationRepository.save(any(Application.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Object result = service.changeStatus(application.getId(),
                new CurrentUser(OWNER_ID, "owner@example.com"),
                new StatusChangeRequest(ApplicationStatus.APPROVED_AND_MEMBER, "Welcome aboard"));

        assertEquals(ApplicationStatus.APPROVED_AND_MEMBER, application.getStatus());
        assertEquals("Welcome aboard", application.getOwnerNote());
        assertThat(result).isNotNull();
        verify(historyRepository).save(any());
        // Acceptance must not create an Organization membership here.
        verify(organizationClient, never()).resolveCompanyId(any());
    }

    @Test
    void ownerApprovalDeniedForDeveloper() {
        Application application = application(ApplicationStatus.OWNER_APPROVAL);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostStatus.OPEN)));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(organizationClient.myMembership(COMPANY_ID))
                .thenReturn(new CallerMembership("DEVELOPER", DEPARTMENT_ID));

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> service.changeStatus(
                application.getId(),
                new CurrentUser(DEVELOPER_ID, "dev@example.com"),
                new StatusChangeRequest(ApplicationStatus.APPROVED_AND_MEMBER, "Sure")));

        assertEquals("OWNER_APPROVAL_REQUIRED", ex.getCode());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void withdrawBlockedAfterFinalApproval() {
        Application application = application(ApplicationStatus.APPROVED_AND_MEMBER);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostStatus.OPEN)));
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        AppException ex = assertThrows(AppException.class, () -> service.changeStatus(
                application.getId(),
                new CurrentUser(APPLICANT_ID, "applicant@example.com"),
                new StatusChangeRequest(ApplicationStatus.WITHDRAWN, "Changed my mind")));

        assertEquals("Cannot withdraw", ex.getMessage());
        verify(applicationRepository, never()).save(any());
    }
}
