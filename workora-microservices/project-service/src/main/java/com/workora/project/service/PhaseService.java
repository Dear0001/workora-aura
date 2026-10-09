package com.workora.project.service;

import com.workora.common.dto.PageResponse;
import com.workora.project.dto.PhaseRequest;
import com.workora.project.dto.PhaseResponse;
import com.workora.project.dto.PhaseUpdateRequest;
import com.workora.project.dto.PublicPhaseResponse;
import com.workora.project.entity.Phase;
import com.workora.project.entity.PhaseStatus;
import com.workora.project.entity.PhaseVisibility;
import com.workora.project.entity.Project;
import com.workora.project.event.ProjectEventPublisher;
import com.workora.project.exception.AppException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.PhaseRepository;
import com.workora.project.repository.ProjectRepository;
import com.workora.project.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PhaseService {

    private final PhaseRepository phaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectAuthorization authorization;
    private final ProjectMapper mapper;
    private final ProjectEventPublisher projectEventPublisher;

    public PhaseService(PhaseRepository phaseRepository,
                        ProjectRepository projectRepository,
                        ProjectService projectService,
                        ProjectAuthorization authorization,
                        ProjectMapper mapper,
                        ProjectEventPublisher projectEventPublisher) {
        this.phaseRepository = phaseRepository;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.authorization = authorization;
        this.mapper = mapper;
        this.projectEventPublisher = projectEventPublisher;
    }

    @Transactional
    public PhaseResponse create(UUID projectId, CurrentUser caller, PhaseRequest request) {
        Project project = projectService.loadActive(projectId);
        authorization.requireCanManageProject(project, caller);
        Phase phase = new Phase(projectId, request.name().trim());
        phase.setDescription(request.description());
        phase.setVisibility(request.visibility() != null ? request.visibility() : PhaseVisibility.PRIVATE);
        Phase saved = phaseRepository.save(phase);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phaseId", saved.getId());
        if (saved.getName() != null) {
            payload.put("name", saved.getName());
        }
        projectEventPublisher.publish(
                "phase.created",
                projectId,
                project.getCompanyId(),
                caller.userId(),
                null,
                payload
        );
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PhaseResponse> listForProject(UUID projectId, CurrentUser caller, String search, Pageable pageable) {
        Project project = projectService.loadActive(projectId);
        authorization.requireCompanyMember(project.getCompanyId());
        String normalized = search == null || search.isBlank() ? null : search.trim();
        Page<Phase> page = normalized == null
                ? phaseRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, PhaseStatus.PUBLISHED, pageable)
                : phaseRepository.findByProjectIdAndStatusAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
                        projectId, PhaseStatus.PUBLISHED, normalized, pageable);
        return PageResponse.of(
                page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<PhaseResponse> listForProject(UUID projectId, CurrentUser caller) {
        Project project = projectService.loadActive(projectId);
        authorization.requireCompanyMember(project.getCompanyId());
        return phaseRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PhaseResponse get(UUID phaseId, CurrentUser caller) {
        Phase phase = load(phaseId);
        Project project = projectService.loadActive(phase.getProjectId());
        authorization.requireCompanyMember(project.getCompanyId());
        return mapper.toResponse(phase);
    }

    @Transactional
    public PhaseResponse update(UUID phaseId, CurrentUser caller, PhaseUpdateRequest request) {
        Phase phase = load(phaseId);
        Project project = projectService.loadActive(phase.getProjectId());
        authorization.requireCanManageProject(project, caller);

        if (request.name() != null && !request.name().isBlank()) {
            phase.setName(request.name().trim());
        }
        if (request.description() != null) {
            phase.setDescription(request.description());
        }
        if (request.visibility() != null) {
            phase.setVisibility(request.visibility());
        }
        if (request.status() != null && request.status() != phase.getStatus()) {
            applyStatusTransition(phase, request.status());
        }
        Phase saved = phaseRepository.save(phase);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phaseId", saved.getId());
        if (saved.getStatus() != null) {
            payload.put("status", saved.getStatus().name());
        }
        projectEventPublisher.publish(
                "phase.updated",
                phase.getProjectId(),
                project.getCompanyId(),
                caller.userId(),
                null,
                payload
        );
        return mapper.toResponse(saved);
    }

    private void applyStatusTransition(Phase phase, PhaseStatus target) {
        PhaseStatus current = phase.getStatus();
        boolean allowed = switch (current) {
            case DRAFT -> target == PhaseStatus.PUBLISHED || target == PhaseStatus.ARCHIVED;
            case PUBLISHED -> target == PhaseStatus.CLOSED || target == PhaseStatus.DRAFT
                    || target == PhaseStatus.ARCHIVED;
            case CLOSED -> target == PhaseStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) {
            throw new AppException("Cannot transition a phase from " + current + " to " + target);
        }
        Instant now = Instant.now();
        if (target == PhaseStatus.PUBLISHED && phase.getPublishedAt() == null) {
            phase.setPublishedAt(now);
        }
        if (target == PhaseStatus.CLOSED) {
            phase.setClosedAt(now);
        }
        phase.setStatus(target);
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicPhaseResponse> listPublic(Pageable pageable) {
        Page<Phase> page = phaseRepository.findByStatusAndVisibility(
                PhaseStatus.PUBLISHED, PhaseVisibility.PUBLIC, pageable);
        List<PublicPhaseResponse> content = page.getContent().stream()
                .map(this::toPublicResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private PublicPhaseResponse toPublicResponse(Phase phase) {
        Project project = projectRepository.findById(phase.getProjectId()).orElse(null);
        return new PublicPhaseResponse(
                phase.getId(),
                phase.getName(),
                phase.getDescription(),
                phase.getProjectId(),
                project != null ? project.getName() : null,
                project != null ? project.getCompanyId() : null,
                phase.getPublishedAt()
        );
    }

    @Transactional(readOnly = true)
    public Phase load(UUID phaseId) {
        return phaseRepository.findById(phaseId)
                .orElseThrow(() -> new NotFoundException("Phase not found"));
    }
}
