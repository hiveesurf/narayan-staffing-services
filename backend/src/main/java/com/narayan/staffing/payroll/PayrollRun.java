package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.domain.PayrollStatus;
import com.narayan.staffing.identity.User;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "period_month", "period_year"}))
public class PayrollRun extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false)
    private int periodMonth;
    @Column(nullable = false)
    private int periodYear;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.DRAFT;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;
    private Instant approvedAt;
    private Instant lockedAt;
}
