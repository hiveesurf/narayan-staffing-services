package com.narayan.staffing.workforce;

import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.EmploymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String employeeCode;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
    @Column(nullable = false, length = 150)
    private String fullName;
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
    @Column(length = 500)
    private String photoPath;
    @Column(length = 150)
    private String bankAccountHolder;
    @Column(length = 150)
    private String bankName;
    @Column(columnDefinition = "text")
    private String bankAccountEncrypted;
    @Column(length = 20)
    private String bankIfsc;
    @Column(length = 150)
    private String bankBranch;
    @Column(columnDefinition = "text")
    private String notes;
}
