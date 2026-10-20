package com.narayan.staffing.job;

import com.narayan.staffing.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
    Optional<Job> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByJobCode(String jobCode);
    long countByStatus(JobStatus status);
    long countByCompanyIdAndStatus(UUID companyId, JobStatus status);
}
