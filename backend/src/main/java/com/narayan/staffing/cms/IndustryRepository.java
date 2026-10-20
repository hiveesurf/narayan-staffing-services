package com.narayan.staffing.cms;

import com.narayan.staffing.domain.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndustryRepository extends JpaRepository<Industry, UUID> {
    Optional<Industry> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Industry> findByActiveTrueOrderByDisplayOrderAsc();
    List<Industry> findAllByOrderByDisplayOrderAsc();
}
