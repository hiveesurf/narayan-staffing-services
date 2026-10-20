package com.narayan.staffing.recruitment;

import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.InterviewMode;
import com.narayan.staffing.domain.InterviewStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class RecruitmentDtos {
    private RecruitmentDtos() {
    }

    public record ApplyRequest(
            @NotBlank String fullName,
            LocalDate dateOfBirth,
            String gender,
            @NotBlank String mobile,
            @NotBlank @Email String email,
            String alternatePhone,
            String address,
            String city,
            String state,
            String pincode,
            String highestQualification,
            String specialization,
            BigDecimal totalExperience,
            BigDecimal relevantExperience,
            String currentCompany,
            String currentJobTitle,
            BigDecimal currentSalary,
            BigDecimal expectedSalary,
            String noticePeriod,
            String skills,
            @AssertTrue(message = "Please confirm the information is accurate.") boolean consent
    ) {
    }

    public record ApplyResponse(String applicationNumber, String message) {
    }

    public record ApplicationCard(
            UUID id, String applicationNumber, String candidateName, String jobTitle, String location,
            BigDecimal experience, BigDecimal expectedSalary, ApplicationStatus status, Instant appliedAt, UUID candidateId, UUID jobId,
            String email, String mobile, String qualification, UUID resumeDocumentId
    ) {
    }

    public record ApplicationDetail(
            UUID id, String applicationNumber, ApplicationStatus status, boolean consent, String coverNote,
            UUID assignedHrId, String assignedHrName, Instant createdAt,
            CandidateView candidate, JobDtosLite job, List<HistoryItem> history, List<NoteItem> notes
    ) {
    }

    public record JobDtosLite(UUID id, String title, String jobCode, String location, String companyName) {
    }

    public record CandidateView(
            UUID id, String fullName, LocalDate dateOfBirth, String gender, String mobile, String email,
            String alternatePhone, String address, String city, String state, String pincode, String country,
            String permanentAddress, String fatherName, String motherName, String maritalStatus,
            String emergencyContactName, String emergencyContactPhone, String highestQualification, String specialization,
            BigDecimal totalExperience, BigDecimal relevantExperience, String currentCompany, String currentJobTitle,
            BigDecimal currentSalary, BigDecimal expectedSalary, String noticePeriod, String photoPath,
            String aadhaar, String pan, String otherId, List<String> skills,
            List<EducationItem> education, List<ExperienceItem> experience
    ) {
    }

    public record EducationItem(UUID id, String degree, String specialization, String institution, Integer passingYear, String score) {
    }

    public record ExperienceItem(UUID id, String company, String designation, LocalDate joiningDate, LocalDate leavingDate, BigDecimal salary, String responsibilities) {
    }

    public record HistoryItem(UUID id, ApplicationStatus oldStatus, ApplicationStatus newStatus, String changedBy, Instant changedAt, String reason, String note) {
    }

    public record NoteItem(UUID id, String author, String note, Instant createdAt) {
    }

    public record StatusChange(@NotNull ApplicationStatus status, String reason, String note) {
    }

    public record BulkStatus(@NotEmpty List<UUID> ids, @NotNull ApplicationStatus status, String note) {
    }

    public record AssignHr(@NotNull UUID userId) {
    }

    public record NoteRequest(@NotBlank String note) {
    }

    public record CandidateUpdate(
            @NotBlank String fullName, LocalDate dateOfBirth, String gender, @NotBlank String mobile, @NotBlank @Email String email,
            String alternatePhone, String address, String city, String state, String pincode, String country, String permanentAddress,
            String fatherName, String motherName, String maritalStatus, String emergencyContactName, String emergencyContactPhone,
            String highestQualification, String specialization, BigDecimal totalExperience, BigDecimal relevantExperience,
            String currentCompany, String currentJobTitle, BigDecimal currentSalary, BigDecimal expectedSalary, String noticePeriod,
            String otherId, String aadhaar, String pan, List<String> skills, List<EducationItem> education, List<ExperienceItem> experience
    ) {
    }

    public record InterviewRequest(
            @NotNull UUID applicationId, UUID interviewerId, @NotNull LocalDate date, @NotNull LocalTime time,
            @NotNull InterviewMode mode, String meetingLink, String location, String remarks
    ) {
    }

    public record InterviewResponse(
            UUID id, UUID applicationId, String candidateName, String jobTitle, String interviewerName,
            LocalDate date, LocalTime time, InterviewMode mode, String meetingLink, String location, String remarks,
            InterviewStatus status, Feedback feedback
    ) {
    }

    public record Feedback(Integer rating, String strengths, String weaknesses, String recommendation, String remarks) {
    }

    public record InterviewStatusUpdate(@NotNull InterviewStatus status) {
    }
}
