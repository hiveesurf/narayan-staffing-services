package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.identity.User;
import com.narayan.staffing.workforce.Employee;
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
@Table(name = "salary_structures")
public class SalaryStructure extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @Column(nullable = false)
    private LocalDate effectiveDate;
    private LocalDate endDate;
    @Column(length = 500)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    @Column(nullable = false)
    private boolean active = true;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;
}
