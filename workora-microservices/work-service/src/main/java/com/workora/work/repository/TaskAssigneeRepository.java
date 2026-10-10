package com.workora.work.repository;

import com.workora.work.entity.TaskAssignee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAssigneeRepository extends JpaRepository<TaskAssignee, UUID> {

    List<TaskAssignee> findByTaskIdAndRemovedAtIsNull(UUID taskId);

    List<TaskAssignee> findByTaskId(UUID taskId);

    List<TaskAssignee> findByUserIdAndRemovedAtIsNull(UUID userId);

    Optional<TaskAssignee> findByTaskIdAndUserIdAndRemovedAtIsNull(UUID taskId, UUID userId);

    boolean existsByTaskIdAndUserIdAndRemovedAtIsNull(UUID taskId, UUID userId);
}
