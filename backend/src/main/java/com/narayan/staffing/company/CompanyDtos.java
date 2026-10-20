package com.narayan.staffing.company;

import com.narayan.staffing.domain.CompanyStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CompanyDtos {
    private CompanyDtos() {
    }

    public record CompanyRequest(
            @NotBlank @Size(max = 200) String companyName,
            @Size(max = 200) String legalName,
            @Size(max = 120) String industry,
            @Size(max = 80) String companyType,
            @Size(max = 255) String website,
            @Email @Size(max = 180) String email,
            @Size(max = 30) String phone,
            String address,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 100) String country,
            @Size(max = 12) String pincode,
            @Size(max = 20) String gstNumber,
            @Size(max = 20) String pan,
            @Size(max = 150) String contactPerson,
            @Size(max = 120) String contactPersonDesignation,
            @Size(max = 30) String contactPersonPhone,
            @Email @Size(max = 180) String contactPersonEmail,
            String billingAddress,
            @NotNull CompanyStatus status,
            String notes
    ) {
    }

    public record CompanyResponse(
            UUID id, String companyCode, String companyName, String legalName, String industry, String companyType,
            String website, String email, String phone, String address, String city, String state, String country,
            String pincode, String gstNumber, String pan, String contactPerson, String contactPersonDesignation,
            String contactPersonPhone, String contactPersonEmail, String billingAddress, CompanyStatus status,
            String notes, String logoPath, List<String> coverPaths, Instant createdAt, Instant updatedAt
    ) {
    }

    public record StatusRequest(@NotNull CompanyStatus status) {
    }

    public record CompanyProfile(
            CompanyResponse company,
            long openJobs,
            long activeEmployees,
            long placements,
            BigDecimal payrollNet,
            List<NamedCount> recentJobs
    ) {
    }

    public record NamedCount(UUID id, String label, String status) {
    }
}
