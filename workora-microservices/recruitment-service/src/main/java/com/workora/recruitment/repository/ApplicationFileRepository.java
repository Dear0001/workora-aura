package com.workora.recruitment.repository;

import com.workora.recruitment.entity.ApplicationFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationFileRepository extends JpaRepository<ApplicationFile, UUID> {

    List<ApplicationFile> findByApplicationId(UUID applicationId);
}
