package com.narayan.staffing.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {
    List<CandidateSkill> findByCandidateIdOrderBySkillNameAsc(UUID candidateId);
    void deleteByCandidateId(UUID candidateId);
}
