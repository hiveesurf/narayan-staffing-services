package com.narayan.staffing.cms;

import com.narayan.staffing.domain.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {
    Optional<ServiceOffering> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<ServiceOffering> findByActiveTrueOrderByDisplayOrderAsc();
    List<ServiceOffering> findAllByOrderByDisplayOrderAsc();
}
