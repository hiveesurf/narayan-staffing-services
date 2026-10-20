package com.narayan.staffing.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobCategoryRepository extends JpaRepository<JobCategory, UUID> {
    Optional<JobCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
