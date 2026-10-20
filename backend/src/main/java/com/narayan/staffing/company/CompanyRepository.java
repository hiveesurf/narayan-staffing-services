package com.narayan.staffing.company;

import com.narayan.staffing.domain.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {
    long countByStatus(CompanyStatus status);
    boolean existsByCompanyCode(String companyCode);
}
