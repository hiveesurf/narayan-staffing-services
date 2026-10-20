package com.narayan.staffing.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentRequirementRepository extends JpaRepository<DocumentRequirement, UUID> {
    List<DocumentRequirement> findByActiveTrueOrderByDisplayOrderAsc();
    List<DocumentRequirement> findAllByOrderByDisplayOrderAsc();
}
