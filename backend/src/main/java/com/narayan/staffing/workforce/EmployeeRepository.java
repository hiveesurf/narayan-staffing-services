package com.narayan.staffing.workforce;

import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PlacementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByCandidateId(UUID candidateId);
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findFirstByFullNameIgnoreCase(String fullName);
    boolean existsByEmployeeCode(String employeeCode);
    long countByStatus(EmployeeStatus status);
    long countByCompanyIdAndStatus(UUID companyId, EmployeeStatus status);
    List<Employee> findByCompanyIdAndStatusIn(UUID companyId, List<EmployeeStatus> statuses);
}
