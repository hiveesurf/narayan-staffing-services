package com.narayan.staffing.workforce;

import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PlacementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlacementStatusEventRepository extends JpaRepository<PlacementStatusEvent, UUID> {
    List<PlacementStatusEvent> findByPlacementIdOrderByChangedAtAsc(UUID placementId);
}
