package com.workora.recruitment.service;

import com.workora.recruitment.client.OrganizationClient;
import com.workora.recruitment.dto.PublicPostResponse;
import com.workora.recruitment.dto.RecruitmentPostRequest;
import com.workora.recruitment.dto.RecruitmentPostResponse;
import com.workora.recruitment.dto.RecruitmentPostUpdateRequest;
import com.workora.recruitment.entity.PostStatus;
import com.workora.recruitment.entity.RecruitmentPost;
import com.workora.recruitment.exception.AppException;
import com.workora.recruitment.exception.NotFoundException;
import com.workora.recruitment.mapper.RecruitmentMapper;
import com.workora.recruitment.repository.RecruitmentPostRepository;
import com.workora.recruitment.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RecruitmentPostService {

    private final RecruitmentPostRepository postRepository;
    private final OrganizationClient organizationClient;
    private final RecruitmentAuthorization authorization;
    private final RecruitmentMapper mapper;

    public RecruitmentPostService(RecruitmentPostRepository postRepository,
                                  OrganizationClient organizationClient,
                                  RecruitmentAuthorization authorization,
                                  RecruitmentMapper mapper) {
        this.postRepository = postRepository;
        this.organizationClient = organizationClient;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    public RecruitmentPostResponse create(CurrentUser caller, RecruitmentPostRequest request) {
        if (request.departmentId() == null) {
            throw new AppException("departmentId is required");
        }
        UUID companyId = organizationClient.resolveCompanyId(request.departmentId());
        authorization.requireCanPublish(companyId, caller);

        RecruitmentPost post = new RecruitmentPost();
        post.setCompanyId(companyId);
        post.setDepartmentId(request.departmentId());
        post.setProjectId(request.projectId());
        post.setTitle(request.title());
        post.setDescription(request.description());
        post.setFee(request.fee());
        post.setCurrency(request.currency());
        post.setImageUrl(request.imageUrl());
        post.setIntendedRole(request.intendedRole());
        post.setAuthorId(caller.userId());
        post.setStatus(PostStatus.DRAFT);
        return mapper.toResponse(postRepository.save(post));
    }

    public RecruitmentPostResponse publish(UUID postId, CurrentUser caller) {
        RecruitmentPost post = loadPost(postId);
        authorization.requireCanManagePost(post, caller);
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new AppException("Only a draft post can be published");
        }
        post.setStatus(PostStatus.OPEN);
        return mapper.toResponse(postRepository.save(post));
    }

    public RecruitmentPostResponse get(UUID postId, CurrentUser caller) {
        RecruitmentPost post = loadPost(postId);
        // OPEN posts are public job ads; DRAFT/CLOSED are manager-only.
        if (post.getStatus() != PostStatus.OPEN) {
            authorization.requireCanManagePost(post, caller);
        }
        return mapper.toResponse(post);
    }

    public RecruitmentPostResponse update(UUID postId, CurrentUser caller, RecruitmentPostUpdateRequest request) {
        RecruitmentPost post = loadPost(postId);
        authorization.requireCanManagePost(post, caller);
        if (request.title() != null) {
            post.setTitle(request.title());
        }
        if (request.description() != null) {
            post.setDescription(request.description());
        }
        if (request.fee() != null) {
            post.setFee(request.fee());
        }
        if (request.currency() != null) {
            post.setCurrency(request.currency());
        }
        if (request.imageUrl() != null) {
            post.setImageUrl(request.imageUrl());
        }
        if (request.intendedRole() != null) {
            post.setIntendedRole(request.intendedRole());
        }
        return mapper.toResponse(postRepository.save(post));
    }

    public RecruitmentPostResponse close(UUID postId, CurrentUser caller) {
        RecruitmentPost post = loadPost(postId);
        authorization.requireCanManagePost(post, caller);
        if (post.getStatus() == PostStatus.CLOSED) {
            throw new AppException("Post is already closed");
        }
        post.setStatus(PostStatus.CLOSED);
        post.setClosedAt(Instant.now());
        return mapper.toResponse(postRepository.save(post));
    }

    public void delete(UUID postId, CurrentUser caller) {
        RecruitmentPost post = loadPost(postId);
        authorization.requireCanManagePost(post, caller);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<PublicPostResponse> listByCompany(UUID companyId, CurrentUser caller) {
        return postRepository.findByCompanyId(companyId).stream()
                .map(mapper::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicPostResponse> listPublic(CurrentUser caller) {
        return postRepository.findByStatus(PostStatus.OPEN).stream()
                .map(mapper::toPublicResponse)
                .toList();
    }

    private RecruitmentPost loadPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Recruitment post not found"));
    }
}
