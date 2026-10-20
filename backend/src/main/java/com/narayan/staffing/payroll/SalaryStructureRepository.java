package com.narayan.staffing.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, UUID> {
    List<SalaryStructure> findByEmployeeIdOrderByEffectiveDateDesc(UUID employeeId);
    Optional<SalaryStructure> findFirstByEmployeeIdAndEffectiveDateLessThanEqualAndActiveTrueOrderByEffectiveDateDesc(UUID employeeId, LocalDate date);
    List<SalaryStructure> findByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(UUID employeeId, LocalDate date);
}
