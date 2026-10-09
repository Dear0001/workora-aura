package com.workora.project.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectAssetRepository extends JpaRepository<ProjectAsset, UUID> {

    List<ProjectAsset> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<ProjectAsset> findByProjectIdAndPhaseIdOrderByCreatedAtDesc(UUID projectId, UUID phaseId);
}
