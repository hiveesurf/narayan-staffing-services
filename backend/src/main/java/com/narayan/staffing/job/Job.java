package com.narayan.staffing.job;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.domain.SalaryType;
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
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, unique = true, length = 220)
    private String slug;
    @Column(nullable = false, unique = true, length = 40)
    private String jobCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false, length = 150)
    private String location;
    @Column(length = 120)
    private String department;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmploymentType employmentType;
    @Column(precision = 4, scale = 1)
    private BigDecimal experienceMin;
    @Column(precision = 4, scale = 1)
    private BigDecimal experienceMax;
    @Column(length = 180)
    private String education;
    @Column(precision = 14, scale = 2)
    private BigDecimal salaryMin;
    @Column(precision = 14, scale = 2)
    private BigDecimal salaryMax;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SalaryType salaryType;
    @Column(nullable = false)
    private int vacancies = 1;
    @Column(columnDefinition = "text")
    private String jobDescription;
    @Column(columnDefinition = "text")
    private String responsibilities;
    @Column(columnDefinition = "text")
    private String requiredSkills;
    @Column(columnDefinition = "text")
    private String preferredSkills;
    @Column(columnDefinition = "text")
    private String benefits;
    private LocalDate deadline;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.DRAFT;
    @Column(nullable = false)
    private boolean featured;
    private Instant publishDate;
}
