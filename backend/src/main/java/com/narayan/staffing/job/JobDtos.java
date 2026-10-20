package com.narayan.staffing.job;

import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.domain.SalaryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class JobDtos {
    private JobDtos() {
    }

    public record JobRequest(
            @NotBlank String title,
            UUID categoryId,
            @NotNull UUID companyId,
            @NotBlank String location,
            String department,
            @NotNull EmploymentType employmentType,
            BigDecimal experienceMin,
            BigDecimal experienceMax,
            String education,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            SalaryType salaryType,
            @NotNull Integer vacancies,
            String jobDescription,
            String responsibilities,
            String requiredSkills,
            String preferredSkills,
            String benefits,
            LocalDate deadline,
            boolean featured
    ) {
    }

    public record JobResponse(
            UUID id, String title, String slug, String jobCode, UUID categoryId, String categoryName,
            UUID companyId, String companyName, String location, String department, EmploymentType employmentType,
            BigDecimal experienceMin, BigDecimal experienceMax, String education, BigDecimal salaryMin, BigDecimal salaryMax,
            SalaryType salaryType, int vacancies, String jobDescription, String responsibilities, String requiredSkills,
            String preferredSkills, String benefits, LocalDate deadline, JobStatus status, boolean featured,
            Instant publishDate, Instant createdAt, long applicationCount
    ) {
    }

    public record CategoryRequest(@NotBlank String name, String description, String icon, boolean active, int displayOrder) {
    }

    public record CategoryResponse(UUID id, String name, String slug, String description, String icon, boolean active, int displayOrder, Instant createdAt, Instant updatedAt) {
    }
}
