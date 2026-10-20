package com.narayan.staffing.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateEducationRepository extends JpaRepository<CandidateEducation, UUID> {
    List<CandidateEducation> findByCandidateIdOrderByPassingYearDesc(UUID candidateId);
}
