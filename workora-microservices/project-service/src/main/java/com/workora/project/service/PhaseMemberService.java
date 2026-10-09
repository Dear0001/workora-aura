package com.workora.project.service;

import com.workora.project.dto.PhaseMemberRequest;
import com.workora.project.dto.PhaseMemberResponse;
import com.workora.project.entity.Phase;
import com.workora.project.entity.PhaseMember;
import com.workora.project.entity.Project;
import com.workora.project.exception.ConflictException;
import com.workora.project.exception.ForbiddenException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.PhaseMemberRepository;
import com.workora.project.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PhaseMemberService {

    private final PhaseMemberRepository phaseMemberRepository;
    private final PhaseService phaseService;
    private final ProjectService projectService;
    private final ProjectAuthorization authorization;
    private final ProjectMapper mapper;

    public PhaseMemberService(PhaseMemberRepository phaseMemberRepository,
                              PhaseService phaseService,
                              ProjectService projectService,
                              ProjectAuthorization authorization,
                              ProjectMapper mapper) {
        this.phaseMemberRepository = phaseMemberRepository;
        this.phaseService = phaseService;
        this.projectService = projectService;
        this.authorization = authorization;
        this.mapper = mapper;
    }

    @Transactional
    public PhaseMemberResponse add(UUID phaseId, CurrentUser caller, PhaseMemberRequest request) {
        Phase phase = phaseService.load(phaseId);
        Project project = projectService.loadActive(phase.getProjectId());
        authorization.requireCanManageProject(project, caller);

        if (phaseMemberRepository.existsByPhaseIdAndUserId(phaseId, request.userId())) {
            throw new ConflictException("User is already a member of this phase");
        }
        String email = request.email() != null ? request.email().trim().toLowerCase() : "";
        PhaseMember member = new PhaseMember(phaseId, request.userId(), email, caller.userId());
        return mapper.toResponse(phaseMemberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<PhaseMemberResponse> list(UUID phaseId, CurrentUser caller) {
        Phase phase = phaseService.load(phaseId);
        Project project = projectService.loadActive(phase.getProjectId());
        requireCanViewMembers(project, phaseId, caller);
        return phaseMemberRepository.findByPhaseIdOrderByCreatedAtAsc(phaseId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void remove(UUID phaseId, UUID userId, CurrentUser caller) {
        Phase phase = phaseService.load(phaseId);
        Project project = projectService.loadActive(phase.getProjectId());
        authorization.requireCanManageProject(project, caller);
        PhaseMember member = phaseMemberRepository.findByPhaseIdAndUserId(phaseId, userId)
                .orElseThrow(() -> new NotFoundException("Phase member not found"));
        phaseMemberRepository.delete(member);
    }

    private void requireCanViewMembers(Project project, UUID phaseId, CurrentUser caller) {
        if (phaseMemberRepository.existsByPhaseIdAndUserId(phaseId, caller.userId())) {
            return;
        }
        var membership = authorization.requireCompanyMember(project.getCompanyId());
        boolean isProjectManager = project.getProjectManagerId() != null
                && project.getProjectManagerId().equals(caller.userId());
        if (!isProjectManager && !membership.isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only phase members or project/company managers can view phase members",
                    "PHASE_MEMBER_VIEW_DENIED");
        }
    }
}
