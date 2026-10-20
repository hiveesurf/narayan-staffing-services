package com.narayan.staffing.workforce;

import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.PlacementStatus;
import com.narayan.staffing.job.Job;
import com.narayan.staffing.recruitment.JobApplication;
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

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "placements")
public class Placement extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private JobApplication application;
    @Column(length = 150)
    private String designation;
    @Column(length = 120)
    private String department;
    private LocalDate joiningDate;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private EmploymentType employmentType;
    @Column(length = 180)
    private String workLocation;
    @Column(length = 150)
    private String reportingManager;
    @Column(precision = 14, scale = 2)
    private BigDecimal agreedSalary;
    @Column(precision = 14, scale = 2)
    private BigDecimal billingSalary;
    @Column(length = 40)
    private String payrollType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlacementStatus placementStatus;
    private LocalDate offerDate;
    @Column(length = 40)
    private String joiningStatus;
    @Column(columnDefinition = "text")
    private String remarks;
}
