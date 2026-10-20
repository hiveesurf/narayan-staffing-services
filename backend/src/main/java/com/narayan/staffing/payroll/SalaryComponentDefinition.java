package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.domain.CalculationType;
import com.narayan.staffing.domain.ComponentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "salary_component_definitions")
public class SalaryComponentDefinition extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComponentType componentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CalculationType calculationType;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal defaultValue = BigDecimal.ZERO;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
