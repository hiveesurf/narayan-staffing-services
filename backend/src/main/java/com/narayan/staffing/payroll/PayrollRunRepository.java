package com.narayan.staffing.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, UUID>, JpaSpecificationExecutor<PayrollRun> {
    Optional<PayrollRun> findByCompanyIdAndPeriodMonthAndPeriodYear(UUID companyId, int month, int year);
    List<PayrollRun> findByCompanyId(UUID companyId);
}
