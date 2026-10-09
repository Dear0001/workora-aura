package com.workora.recruitment.repository;

import com.workora.recruitment.entity.ApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, UUID> {

    List<ApplicationHistory> findByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
