package com.workora.recruitment.repository;

import com.workora.recruitment.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByPostId(UUID postId);

    List<Application> findByApplicantId(UUID applicantId);

    Optional<Application> findByPostIdAndApplicantId(UUID postId, UUID applicantId);

    long countByPostId(UUID postId);
}
