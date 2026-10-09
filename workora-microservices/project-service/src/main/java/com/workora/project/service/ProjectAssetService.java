package com.workora.project.service;

import com.workora.project.asset.ProjectAsset;
import com.workora.project.asset.ProjectAssetRepository;
import com.workora.project.dto.ProjectAssetRequest;
import com.workora.project.dto.ProjectAssetResponse;
import com.workora.project.entity.Project;
import com.workora.project.exception.AppException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectAssetService {

    private final ProjectAssetRepository assetRepository;
    private final ProjectService projectService;

    public ProjectAssetService(ProjectAssetRepository assetRepository, ProjectService projectService) {
        this.assetRepository = assetRepository;
        this.projectService = projectService;
    }

    @Transactional
    public ProjectAssetResponse create(UUID projectId, CurrentUser caller, ProjectAssetRequest request) {
        Project project = projectService.loadActive(projectId);
        projectService.ensureCanManageProject(project, caller);

        String storageKey = request.storageKey().trim();
        if (!isSafeStorageKey(storageKey)) {
            throw new AppException("Unsafe storage key for project asset");
        }

        ProjectAsset asset = new ProjectAsset(
                projectId,
                request.phaseId(),
                request.name().trim(),
                storageKey,
                request.contentType().trim(),
                request.kind().trim(),
                caller.userId());

        ProjectAsset saved = assetRepository.save(asset);
        return new ProjectAssetResponse(
                saved.getId(),
                saved.getProjectId(),
                saved.getPhaseId(),
                saved.getName(),
                saved.getStorageKey(),
                saved.getContentType(),
                saved.getKind(),
                saved.getUploadedBy(),
                saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ProjectAssetResponse> list(UUID projectId, CurrentUser caller) {
        Project project = projectService.loadActive(projectId);
        projectService.ensureCanManageProject(project, caller);
        return assetRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(asset -> new ProjectAssetResponse(
                        asset.getId(),
                        asset.getProjectId(),
                        asset.getPhaseId(),
                        asset.getName(),
                        asset.getStorageKey(),
                        asset.getContentType(),
                        asset.getKind(),
                        asset.getUploadedBy(),
                        asset.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectAssetResponse get(UUID assetId, UUID projectId, CurrentUser caller) {
        Project project = projectService.loadActive(projectId);
        projectService.ensureCanManageProject(project, caller);
        ProjectAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("Project asset not found"));
        if (!asset.getProjectId().equals(projectId)) {
            throw new NotFoundException("Project asset not found");
        }
        return new ProjectAssetResponse(
                asset.getId(),
                asset.getProjectId(),
                asset.getPhaseId(),
                asset.getName(),
                asset.getStorageKey(),
                asset.getContentType(),
                asset.getKind(),
                asset.getUploadedBy(),
                asset.getCreatedAt());
    }

    private boolean isSafeStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        String normalized = storageKey.replace('\\', '/');
        return !normalized.contains("..")
                && !normalized.startsWith("/")
                && !normalized.contains("://")
                && normalized.startsWith("projects/");
    }
}
