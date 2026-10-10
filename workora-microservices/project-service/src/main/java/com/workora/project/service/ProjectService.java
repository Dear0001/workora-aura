package com.workora.project.service;

import com.workora.common.dto.PageResponse;
import com.workora.project.client.OrganizationClient;
import com.workora.project.dto.ProjectRequest;
import com.workora.project.dto.ProjectResponse;
import com.workora.project.dto.ProjectUpdateRequest;
import com.workora.project.entity.Project;
import com.workora.project.entity.ProjectStatus;
import com.workora.project.event.ProjectEventPublisher;
import com.workora.project.exception.ConflictException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.ProjectRepository;
import com.workora.project.repository.PhaseMemberRepository;
import com.workora.project.dto.PhaseMemberResponse;
import com.workora.project.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(ProjectStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ProjectStatus.ACTIVE, EnumSet.of(ProjectStatus.ARCHIVED));
        ALLOWED_TRANSITIONS.put(ProjectStatus.ARCHIVED, EnumSet.noneOf(ProjectStatus.class));
    }

    private final ProjectRepository projectRepository;
    private final OrganizationClient organizationClient;
    private final ProjectAuthorization authorization;
    private final ProjectMapper mapper;
    private final ProjectEventPublisher projectEventPublisher;
    private final PhaseMemberRepository phaseMemberRepository;

    public ProjectService(ProjectRepository projectRepository,
                          OrganizationClient organizationClient,
                          ProjectAuthorization authorization,
                          ProjectMapper mapper,
                          ProjectEventPublisher projectEventPublisher,
                          PhaseMemberRepository phaseMemberRepository) {
        this.projectRepository = projectRepository;
        this.organizationClient = organizationClient;
        this.authorization = authorization;
        this.mapper = mapper;
        this.projectEventPublisher = projectEventPublisher;
        this.phaseMemberRepository = phaseMemberRepository;
    }

    @Transactional
    public ProjectResponse createInDepartment(UUID departmentId, CurrentUser caller, ProjectRequest request) {
        UUID companyId = organizationClient.resolveCompanyId(departmentId);
        authorization.requireCanCreateInDepartment(departmentId, companyId);

        String name = request.name().trim();
        if (projectRepository.existsByDepartmentIdAndNameAndStatus(departmentId, name, ProjectStatus.ACTIVE)) {
            throw new ConflictException("A project with this name already exists in the department");
        }
        UUID managerId = request.projectManagerId() != null ? request.projectManagerId() : caller.userId();
        if (request.projectManagerId() != null) {
            authorization.requireCanAssignProjectManager(companyId);
            authorization.requireEligibleProjectManager(companyId, departmentId, managerId);
        }
        Project project = new Project(companyId, departmentId, name, managerId);
        project.setDescription(request.description());
        project.setDueDate(request.dueDate());
        project.setProgress(request.progress() == null ? 0 : request.progress());
        Project saved = projectRepository.save(project);
        Map<String, Object> details = new HashMap<>();
        details.put("name", saved.getName());
        details.put("departmentId", departmentId);
        details.put("dueDate", saved.getDueDate() == null ? "" : saved.getDueDate().toString());
        details.put("progress", saved.getProgress());
        projectEventPublisher.publish(
                "project.created",
                saved.getId(),
                companyId,
                caller.userId(),
                managerId,
                details
        );
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, CurrentUser caller) {
        Project project = loadActive(projectId);
        if (!authorization.canViewProject(project, caller)) {
            throw new com.workora.project.exception.ForbiddenException(
                    "You are not authorized to view this project", "PROJECT_VIEW_DENIED");
        }
        return mapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listByCompany(UUID companyId, CurrentUser caller, String search, Pageable pageable) {
        var membership = authorization.requireCompanyMember(companyId);
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<Project> page;
        if (membership.isOwnerOrAdmin()) {
            page = normalizedSearch == null
                    ? projectRepository.findByCompanyIdAndStatus(companyId, ProjectStatus.ACTIVE, pageable)
                    : projectRepository.findByCompanyIdAndStatusAndNameContainingIgnoreCase(
                            companyId, ProjectStatus.ACTIVE, normalizedSearch, pageable);
        } else if (membership.departmentIds().isEmpty()) {
            page = Page.empty(pageable);
        } else {
            page = normalizedSearch == null
                    ? projectRepository.findByCompanyIdAndDepartmentIdInAndStatus(
                            companyId, membership.departmentIds(), ProjectStatus.ACTIVE, pageable)
                    : projectRepository.findByCompanyIdAndDepartmentIdInAndStatusAndNameContainingIgnoreCase(
                            companyId, membership.departmentIds(), ProjectStatus.ACTIVE, normalizedSearch, pageable);
        }
        return PageResponse.of(
                page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> listByCompany(UUID companyId, CurrentUser caller, Pageable pageable) {
        return listByCompany(companyId, caller, null, pageable);
    }

    @Transactional
    public ProjectResponse update(UUID projectId, CurrentUser caller, ProjectUpdateRequest request) {
        Project project = loadActive(projectId);
        requireCanManageProject(project, caller);
        LocalDate previousDueDate = project.getDueDate();
        int previousProgress = project.getProgress();
        if (request.name() != null && !request.name().isBlank()) {
            String name = request.name().trim();
            if (!name.equals(project.getName())
                    && projectRepository.existsByDepartmentIdAndNameAndStatus(
                            project.getDepartmentId(), name, ProjectStatus.ACTIVE)) {
                throw new ConflictException("A project with this name already exists in the department");
            }
            project.setName(name);
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.projectManagerId() != null) {
            authorization.requireCanAssignProjectManager(project);
            authorization.requireEligibleProjectManager(
                    project.getCompanyId(), project.getDepartmentId(), request.projectManagerId());
            project.setProjectManagerId(request.projectManagerId());
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) {
            project.setDueDate(null);
        } else if (request.dueDate() != null) {
            project.setDueDate(request.dueDate());
        }
        if (request.progress() != null) {
            project.setProgress(request.progress());
        }
        Project saved = projectRepository.save(project);
        Map<String, Object> changes = new HashMap<>();
        changes.put("name", saved.getName());
        if (!java.util.Objects.equals(previousDueDate, saved.getDueDate())) {
            changes.put("dueDate", Map.of(
                    "previous", previousDueDate == null ? "" : previousDueDate.toString(),
                    "current", saved.getDueDate() == null ? "" : saved.getDueDate().toString()));
        }
        if (previousProgress != saved.getProgress()) {
            changes.put("progress", Map.of("previous", previousProgress, "current", saved.getProgress()));
        }
        projectEventPublisher.publish(
                "project.updated",
                saved.getId(),
                project.getCompanyId(),
                caller.userId(),
                project.getProjectManagerId(),
                changes
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public ProjectResponse archive(UUID projectId, CurrentUser caller) {
        Project project = loadActive(projectId);
        authorization.requireCanArchiveProject(project);
        transitionStatus(project, ProjectStatus.ARCHIVED, caller.userId());
        return mapper.toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public java.util.List<PhaseMemberResponse> listMembers(UUID projectId, CurrentUser caller) {
        Project project = loadActive(projectId);
        if (!authorization.canViewProject(project, caller)) {
            throw new com.workora.project.exception.ForbiddenException(
                    "You are not authorized to view this project's members", "PROJECT_MEMBERS_VIEW_DENIED");
        }
        return phaseMemberRepository.findByProjectId(projectId).stream().map(m ->
                new PhaseMemberResponse(m.getId(), m.getPhaseId(), m.getUserId(), m.getEmail(),
                        m.getCreatedAt())).toList();
    }

    @Transactional
    public ProjectResponse assignManager(UUID projectId, UUID managerId, CurrentUser caller) {
        Project project = loadActive(projectId);
        authorization.requireCanAssignProjectManager(project);
        authorization.requireEligibleProjectManager(project.getCompanyId(), project.getDepartmentId(), managerId);
        project.setProjectManagerId(managerId);
        return mapper.toResponse(projectRepository.save(project));
    }

    public void ensureCanManageProject(Project project, CurrentUser caller) {
        requireCanManageProject(project, caller);
    }

    public void requireCanManageProject(Project project, CurrentUser caller) {
        authorization.requireCanManageProject(project, caller);
    }

    @Transactional(readOnly = true)
    public Project loadActive(UUID projectId) {
        return projectRepository.findByIdAndStatus(projectId, ProjectStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private void transitionStatus(Project project, ProjectStatus target, UUID actorId) {
        ProjectStatus current = project.getStatus();
        Set<ProjectStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new IllegalStateException("Invalid project status transition from " + current + " to " + target);
        }
        project.setStatus(target);
        project.setArchivedAt(Instant.now());
        projectEventPublisher.publish(
                "project.archived",
                project.getId(),
                project.getCompanyId(),
                actorId,
                project.getProjectManagerId(),
                Map.of("from", current.name(), "to", target.name())
        );
    }
}
