package com.narayan.staffing.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeSalaryComponentRepository extends JpaRepository<EmployeeSalaryComponent, UUID> {
    List<EmployeeSalaryComponent> findBySalaryStructureId(UUID structureId);
}
