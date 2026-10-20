package com.narayan.staffing.recruitment;

import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.InterviewMode;
import com.narayan.staffing.domain.InterviewStatus;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interviews")
public class Interview extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interviewer_id")
    private User interviewer;
    @Column(nullable = false)
    private LocalDate scheduledDate;
    @Column(nullable = false)
    private LocalTime scheduledTime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewMode mode;
    @Column(length = 500)
    private String meetingLink;
    @Column(length = 255)
    private String location;
    @Column(columnDefinition = "text")
    private String remarks;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewStatus status = InterviewStatus.SCHEDULED;
}
