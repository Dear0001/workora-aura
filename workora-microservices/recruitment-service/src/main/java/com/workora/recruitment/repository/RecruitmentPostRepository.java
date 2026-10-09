package com.workora.recruitment.repository;

import com.workora.recruitment.entity.PostStatus;
import com.workora.recruitment.entity.RecruitmentPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecruitmentPostRepository extends JpaRepository<RecruitmentPost, UUID> {

    List<RecruitmentPost> findByCompanyId(UUID companyId);

    List<RecruitmentPost> findByStatus(PostStatus status);

    List<RecruitmentPost> findByStatusAndCompanyId(PostStatus status, UUID companyId);
}
