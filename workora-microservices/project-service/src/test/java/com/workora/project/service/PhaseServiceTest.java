package com.workora.project.service;

import com.workora.project.dto.PhaseUpdateRequest;
import com.workora.project.entity.Phase;
import com.workora.project.entity.PhaseStatus;
import com.workora.project.entity.PhaseVisibility;
import com.workora.project.entity.Project;
import com.workora.project.exception.AppException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.PhaseRepository;
import com.workora.project.repository.ProjectRepository;
import com.workora.project.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhaseServiceTest {

    @Mock
    private PhaseRepository phaseRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectAuthorization authorization;
    @Mock
    private ProjectMapper mapper;
    @Mock
    private com.workora.project.event.ProjectEventPublisher projectEventPublisher;

    private PhaseService service;

    private final UUID projectId = UUID.randomUUID();
    private final UUID phaseId = UUID.randomUUID();
    private final CurrentUser caller = new CurrentUser(UUID.randomUUID(), "pm@workora.test");

    @BeforeEach
    void setUp() {
        service = new PhaseService(phaseRepository, projectRepository, projectService, authorization, mapper, projectEventPublisher);
    }

    private Phase phaseWithStatus(PhaseStatus status) {
        Phase phase = new Phase(projectId, "Phase 1");
        phase.setStatus(status);
        phase.setVisibility(PhaseVisibility.PUBLIC);
        return phase;
    }

    private void stubManageAllowed(Phase phase) {
        Project project = new Project(UUID.randomUUID(), UUID.randomUUID(), "Project", caller.userId());
        lenient().when(projectService.loadActive(projectId)).thenReturn(project);
        lenient().when(phaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
        lenient().when(phaseRepository.save(any(Phase.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mapper.toResponse(any(Phase.class))).thenReturn(null);
    }

    @Test
    void publishDraftPhase_setsPublishedAt() {
        Phase phase = phaseWithStatus(PhaseStatus.DRAFT);
        stubManageAllowed(phase);

        service.update(phaseId, caller, new PhaseUpdateRequest(null, null, null, PhaseStatus.PUBLISHED));

        ArgumentCaptor<Phase> captor = ArgumentCaptor.forClass(Phase.class);
        verify(phaseRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PhaseStatus.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void draftToClosed_isRejected() {
        Phase phase = phaseWithStatus(PhaseStatus.DRAFT);
        stubManageAllowed(phase);

        assertThatThrownBy(() -> service.update(phaseId, caller,
                new PhaseUpdateRequest(null, null, null, PhaseStatus.CLOSED)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void archivedPhase_cannotTransition() {
        Phase phase = phaseWithStatus(PhaseStatus.ARCHIVED);
        stubManageAllowed(phase);

        assertThatThrownBy(() -> service.update(phaseId, caller,
                new PhaseUpdateRequest(null, null, null, PhaseStatus.PUBLISHED)))
                .isInstanceOf(AppException.class);
    }

    @Test
    void publishedToClosed_setsClosedAt() {
        Phase phase = phaseWithStatus(PhaseStatus.PUBLISHED);
        stubManageAllowed(phase);

        service.update(phaseId, caller, new PhaseUpdateRequest(null, null, null, PhaseStatus.CLOSED));

        assertThat(phase.getStatus()).isEqualTo(PhaseStatus.CLOSED);
        assertThat(phase.getClosedAt()).isNotNull();
    }
}
