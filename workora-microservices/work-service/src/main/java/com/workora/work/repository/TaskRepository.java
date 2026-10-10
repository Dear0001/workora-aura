package com.workora.work.repository;

import com.workora.work.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByPhaseIdAndArchivedAtIsNull(UUID phaseId);

    List<Task> findByProjectId(UUID projectId);

    List<Task> findByCompanyIdAndArchivedAtIsNullOrderByCreatedAtDesc(UUID companyId);

    java.util.Optional<Task> findByIdAndArchivedAtIsNull(UUID taskId);
}
