package com.narayan.staffing.workforce;

import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.PlacementStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class WorkforceDtos {
    private WorkforceDtos() {
    }

    public record PlacementRequest(
            @NotNull UUID candidateId,
            @NotNull UUID companyId,
            UUID jobId,
            UUID applicationId,
            String designation,
            String department,
            LocalDate joiningDate,
            EmploymentType employmentType,
            String workLocation,
            String reportingManager,
            BigDecimal agreedSalary,
            BigDecimal billingSalary,
            String payrollType,
            @NotNull PlacementStatus placementStatus,
            LocalDate offerDate,
            String joiningStatus,
            String remarks
    ) {
    }

    public record PlacementResponse(
            UUID id, UUID candidateId, String candidateName, UUID companyId, String companyName, UUID jobId, String jobTitle,
            UUID employeeId, String employeeCode, UUID applicationId, String designation, String department, LocalDate joiningDate,
            EmploymentType employmentType, String workLocation, String reportingManager, BigDecimal agreedSalary, BigDecimal billingSalary,
            String payrollType, PlacementStatus placementStatus, LocalDate offerDate, String joiningStatus, String remarks, Instant createdAt
    ) {
    }

    public record StatusNote(@NotNull PlacementStatus status, String reason, String note) {
    }

    public record EmployeeResponse(
            UUID id, String employeeCode, UUID candidateId, UUID companyId, String companyName, String fullName, String designation,
            String department, LocalDate joiningDate, EmploymentType employmentType, String workLocation, String reportingManager,
            EmployeeStatus status, String photoPath, String bankAccountHolder, String bankName, String bankAccount, String bankIfsc,
            String bankBranch, String notes, BigDecimal currentNet, Instant createdAt
    ) {
    }

    public record EmployeeCreate(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 20) String mobile,
            @NotBlank @Email @Size(max = 180) String email,
            String address,
            @Size(max = 100) String city,
            @NotNull UUID companyId,
            @Size(max = 150) String designation,
            @Size(max = 120) String department,
            @NotNull LocalDate joiningDate,
            EmploymentType employmentType,
            @Size(max = 180) String workLocation,
            @Size(max = 150) String reportingManager,
            String notes
    ) {
    }

    public record EmployeeUpdate(
            @NotBlank @Size(max = 150) String fullName,
            @NotBlank @Size(max = 20) String mobile,
            @NotBlank @Email @Size(max = 180) String email,
            String address,
            @Size(max = 100) String city,
            UUID companyId,
            @Size(max = 150) String designation,
            @Size(max = 120) String department,
            LocalDate joiningDate,
            EmploymentType employmentType,
            @Size(max = 180) String workLocation,
            @Size(max = 150) String reportingManager,
            String notes
    ) {
    }

    public record BankUpdate(@NotBlank String accountHolder, @NotBlank String bankName, @NotBlank String accountNumber, @NotBlank String ifsc, String branch) {
    }

    public record EmployeeStatusUpdate(@NotNull EmployeeStatus status, String reason, String note) {
    }

    public record HistoryItem(String oldStatus, String newStatus, String changedBy, Instant changedAt, String reason, String note) {
    }

    public record EmployeeProfile(EmployeeResponse employee, com.narayan.staffing.recruitment.RecruitmentDtos.CandidateView candidate,
                                  List<PlacementResponse> placements, List<HistoryItem> statusHistory, HireInfo hire) {
    }

    public record HireInfo(String jobTitle, String jobCode, String applicationNumber, String applicationStatus,
                           UUID resumeDocumentId, String resumeFileName) {
    }
}
