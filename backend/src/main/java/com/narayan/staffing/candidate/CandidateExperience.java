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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_experience")
public class CandidateExperience extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    @Column(nullable = false, length = 180)
    private String company;
    @Column(length = 150)
    private String designation;
    private LocalDate joiningDate;
    private LocalDate leavingDate;
    @Column(precision = 14, scale = 2)
    private BigDecimal salary;
    @Column(columnDefinition = "text")
    private String responsibilities;
}
