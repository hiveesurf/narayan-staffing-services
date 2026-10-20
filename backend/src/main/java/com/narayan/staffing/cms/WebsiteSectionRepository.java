package com.narayan.staffing.cms;

import com.narayan.staffing.domain.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsiteSectionRepository extends JpaRepository<WebsiteSection, UUID> {
    List<WebsiteSection> findByPageKeyOrderByDisplayOrderAsc(String pageKey);
    Optional<WebsiteSection> findByPageKeyAndSectionKey(String pageKey, String sectionKey);
}
