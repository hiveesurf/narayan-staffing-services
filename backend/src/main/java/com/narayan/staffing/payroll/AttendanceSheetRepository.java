package com.narayan.staffing.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceSheetRepository extends JpaRepository<AttendanceSheet, UUID> {
    Optional<AttendanceSheet> findByCompanyIdAndPeriodMonthAndPeriodYear(UUID companyId, int periodMonth, int periodYear);
}
