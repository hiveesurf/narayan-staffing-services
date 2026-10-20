package com.narayan.staffing.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceDayRepository extends JpaRepository<AttendanceDay, UUID> {
    List<AttendanceDay> findBySheetId(UUID sheetId);
}
