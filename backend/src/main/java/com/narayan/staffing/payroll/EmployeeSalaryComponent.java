package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.CalculationType;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employee_salary_components")
public class EmployeeSalaryComponent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private SalaryComponentDefinition component;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalculationType calculationType;
    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal value;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal calculatedAmount = BigDecimal.ZERO;
}
