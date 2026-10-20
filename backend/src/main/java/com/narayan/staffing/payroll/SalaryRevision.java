package com.narayan.staffing.payroll;

import com.narayan.staffing.identity.User;
import com.narayan.staffing.workforce.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "salary_revisions")
public class SalaryRevision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_structure_id")
    private SalaryStructure previousStructure;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "new_structure_id", nullable = false)
    private SalaryStructure newStructure;
    @Column(nullable = false)
    private LocalDate effectiveDate;
    @Column(precision = 14, scale = 2)
    private BigDecimal previousNet;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal newNet;
    @Column(length = 500)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
