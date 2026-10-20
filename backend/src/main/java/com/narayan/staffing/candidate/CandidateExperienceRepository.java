package com.narayan.staffing.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, UUID> {
    List<CandidateExperience> findByCandidateIdOrderByJoiningDateDesc(UUID candidateId);
}
