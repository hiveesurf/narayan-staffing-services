package com.narayan.staffing.recruitment;

import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.identity.User;
import com.narayan.staffing.job.Job;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(columnNames = {"candidate_id", "job_id"}))
public class JobApplication extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String applicationNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApplicationStatus status = ApplicationStatus.APPLIED;
    @Column(nullable = false)
    private boolean consent;
    @Column(columnDefinition = "text")
    private String coverNote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_hr_id")
    private User assignedHr;
}
