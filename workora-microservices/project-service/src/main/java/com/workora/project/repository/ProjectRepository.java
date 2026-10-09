package com.workora.project.repository;

import com.workora.project.entity.Project;
import com.workora.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndStatus(UUID id, ProjectStatus status);

    Page<Project> findByCompanyIdAndStatus(UUID companyId, ProjectStatus status, Pageable pageable);

    Page<Project> findByCompanyIdAndStatusAndNameContainingIgnoreCase(UUID companyId, ProjectStatus status, String name, Pageable pageable);

    Page<Project> findByCompanyIdAndDepartmentIdAndStatus(UUID companyId, UUID departmentId, ProjectStatus status, Pageable pageable);

    Page<Project> findByCompanyIdAndDepartmentIdAndStatusAndNameContainingIgnoreCase(
            UUID companyId, UUID departmentId, ProjectStatus status, String name, Pageable pageable);

    Page<Project> findByCompanyIdAndDepartmentIdInAndStatus(
            UUID companyId, List<UUID> departmentIds, ProjectStatus status, Pageable pageable);

    Page<Project> findByCompanyIdAndDepartmentIdInAndStatusAndNameContainingIgnoreCase(
            UUID companyId, List<UUID> departmentIds, ProjectStatus status, String name, Pageable pageable);

    List<Project> findByDepartmentIdAndStatus(UUID departmentId, ProjectStatus status);

    boolean existsByDepartmentIdAndNameAndStatus(UUID departmentId, String name, ProjectStatus status);
}
