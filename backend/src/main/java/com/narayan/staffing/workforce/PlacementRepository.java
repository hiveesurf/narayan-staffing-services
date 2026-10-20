package com.narayan.staffing.workforce;

import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PlacementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlacementRepository extends JpaRepository<Placement, UUID>, JpaSpecificationExecutor<Placement> {
    List<Placement> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
    List<Placement> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<Placement> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);
    long countByPlacementStatus(PlacementStatus status);
    long countByCompanyIdAndPlacementStatusIn(UUID companyId, List<PlacementStatus> statuses);
}
