package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {
    Optional<InterviewFeedback> findByInterviewId(UUID interviewId);
}
