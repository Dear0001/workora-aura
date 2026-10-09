package com.workora.bug.repository;

import com.workora.bug.entity.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugReportRepository extends JpaRepository<BugReport, UUID> {

    List<BugReport> findByPhaseId(UUID phaseId);

    List<BugReport> findByTaskId(UUID taskId);

    List<BugReport> findByReporterId(UUID reporterId);

    List<BugReport> findByCompanyId(UUID companyId);
}
