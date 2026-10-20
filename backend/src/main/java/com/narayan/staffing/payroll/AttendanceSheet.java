package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BaseEntity;
import com.narayan.staffing.company.Company;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_sheets", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "period_month", "period_year"}))
public class AttendanceSheet extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @Column(nullable = false)
    private int periodMonth;
    @Column(nullable = false)
    private int periodYear;
    @Column(length = 255)
    private String fileName;
}
