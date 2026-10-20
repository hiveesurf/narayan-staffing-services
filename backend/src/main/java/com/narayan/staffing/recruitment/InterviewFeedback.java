package com.narayan.staffing.recruitment;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.identity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "interview_feedback")
public class InterviewFeedback extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private Interview interview;
    private Integer rating;
    @Column(columnDefinition = "text")
    private String strengths;
    @Column(columnDefinition = "text")
    private String weaknesses;
    @Column(length = 30)
    private String recommendation;
    @Column(columnDefinition = "text")
    private String remarks;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;
}
