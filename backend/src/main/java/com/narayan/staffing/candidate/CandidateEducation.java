package com.narayan.staffing.candidate;

import com.narayan.staffing.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_education")
public class CandidateEducation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    @Column(nullable = false, length = 150)
    private String degree;
    @Column(length = 150)
    private String specialization;
    @Column(length = 200)
    private String institution;
    private Integer passingYear;
    @Column(length = 40)
    private String score;
}
