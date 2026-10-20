package com.narayan.staffing.cms;

import com.narayan.staffing.domain.ContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebsiteClientRepository extends JpaRepository<WebsiteClient, UUID> {
    List<WebsiteClient> findByActiveTrueOrderByDisplayOrderAsc();
    List<WebsiteClient> findAllByOrderByDisplayOrderAsc();
}
