package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {
    Optional<JobApplication> findByApplicationNumber(String applicationNumber);
    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
    long countByStatus(ApplicationStatus status);
    long countByCreatedAtBetween(Instant from, Instant to);
    long countByStatusAndCreatedAtBetween(ApplicationStatus status, Instant from, Instant to);
    long countByJobId(UUID jobId);
    List<JobApplication> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
