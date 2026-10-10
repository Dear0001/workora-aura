package com.workora.work.repository;

import com.workora.work.entity.TaskStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskStatusHistoryRepository extends JpaRepository<TaskStatusHistory, UUID> {

    List<TaskStatusHistory> findByTaskIdOrderByChangedAtAsc(UUID taskId);

    List<TaskStatusHistory> findByTaskId(UUID taskId);

    Optional<TaskStatusHistory> findFirstByTaskIdOrderByChangedAtDesc(UUID taskId);
}
