package com.workora.bug.repository;

import com.workora.bug.entity.BugStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugStatusHistoryRepository extends JpaRepository<BugStatusHistory, UUID> {

    List<BugStatusHistory> findByReportIdOrderByChangedAtAsc(UUID reportId);
}
