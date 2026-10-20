package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.workforce.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_records", uniqueConstraints = @UniqueConstraint(columnNames = {"payroll_run_id", "employee_id"}))
public class PayrollRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_structure_id")
    private SalaryStructure salaryStructure;
    @Column(nullable = false, length = 40)
    private String employeeCode;
    @Column(nullable = false, length = 150)
    private String employeeName;
    @Column(length = 150)
    private String designation;
    @Column(length = 120)
    private String department;
    @Column(nullable = false)
    private int totalDays;
    @Column(nullable = false)
    private int workingDays;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal payableDays;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal leaveDays = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal basicAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;
}
