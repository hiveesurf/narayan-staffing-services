package com.narayan.staffing.workforce;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.candidate.CandidateRepository;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.CodeGenerator;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.common.SensitiveDataMasker;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.company.CompanyService;
import com.narayan.staffing.document.DocumentService;
import com.narayan.staffing.document.StoredDocument;
import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PlacementStatus;
import com.narayan.staffing.identity.UserRepository;
import com.narayan.staffing.job.JobService;
import com.narayan.staffing.notification.NotificationService;
import com.narayan.staffing.payroll.SalaryStructureRepository;
import com.narayan.staffing.recruitment.JobApplication;
import com.narayan.staffing.recruitment.JobApplicationRepository;
import com.narayan.staffing.recruitment.RecruitmentService;
import com.narayan.staffing.security.FieldCrypto;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkforceService {

    private final EmployeeRepository employees;
    private final CandidateRepository candidates;
    private final EmployeeStatusEventRepository employeeHistory;
    private final PlacementRepository placements;
    private final PlacementStatusEventRepository placementHistory;
    private final RecruitmentService recruitment;
    private final JobApplicationRepository applications;
    private final DocumentService documents;
    private final CompanyService companies;
    private final JobService jobs;
    private final UserRepository users;
    private final SalaryStructureRepository salaryStructures;
    private final CodeGenerator codes;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final FieldCrypto crypto;

    @Transactional
    public Employee ensureEmployee(UUID candidateId) {
        Candidate candidate = recruitment.requireCandidate(candidateId);
        return employees.findByCandidateId(candidateId).orElseGet(() -> {
            Employee employee = new Employee();
            employee.setEmployeeCode(codes.next("employee_code_seq", "NSS-EMP-", 5));
            employee.setCandidate(candidate);
            employee.setFullName(candidate.getFullName());
            employee.setPhotoPath(candidate.getPhotoPath());
            employee.setStatus(EmployeeStatus.ACTIVE);
            employees.save(employee);
            writeEmployeeStatus(employee, null, EmployeeStatus.ACTIVE, "Created from candidate", null);
            auditService.log("EMPLOYEE_CREATED", "EMPLOYEE", employee.getId().toString(), Map.of("candidateId", candidateId.toString()));
            return employee;
        });
    }

    @Transactional
    public WorkforceDtos.PlacementResponse createPlacement(WorkforceDtos.PlacementRequest request) {
        Candidate candidate = recruitment.requireCandidate(request.candidateId());
        Company company = companies.require(request.companyId());
        Employee employee = ensureEmployee(candidate.getId());
        Placement placement = new Placement();
        placement.setCandidate(candidate);
        placement.setCompany(company);
        placement.setEmployee(employee);
        if (request.jobId() != null) placement.setJob(jobs.require(request.jobId()));
        if (request.applicationId() != null) placement.setApplication(recruitment.requireApplication(request.applicationId()));
        applyPlacement(placement, request);
        placements.save(placement);
        writePlacementStatus(placement, null, placement.getPlacementStatus(), "Placement created", request.remarks());
        syncEmployee(placement);
        auditService.log("PLACEMENT_CREATED", "PLACEMENT", placement.getId().toString(), Map.of("company", company.getCompanyName()));
        if (placement.getPlacementStatus() == PlacementStatus.JOINED || placement.getPlacementStatus() == PlacementStatus.ACTIVE) {
            notifications.email(candidate.getEmail(), "JOINING_REMINDER", "Joining details",
                    "Your placement with " + company.getCompanyName() + " is recorded. Joining date: " + placement.getJoiningDate(), "PLACEMENT", placement.getId());
        }
        return toPlacement(placement);
    }

    @Transactional
    public WorkforceDtos.PlacementResponse updatePlacementStatus(UUID id, PlacementStatus status, String reason, String note) {
        Placement placement = placements.findById(id).orElseThrow(() -> new NotFoundException("Placement not found"));
        if (placement.getPlacementStatus() == PlacementStatus.EXITED || placement.getPlacementStatus() == PlacementStatus.CANCELLED) {
            throw new BusinessException("This placement is closed. Create a new placement instead of reopening it.");
        }
        PlacementStatus previous = placement.getPlacementStatus();
        placement.setPlacementStatus(status);
        writePlacementStatus(placement, previous, status, reason, note);
        syncEmployee(placement);
        return toPlacement(placement);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkforceDtos.PlacementResponse> placements(String q, PlacementStatus status, UUID companyId, int page, int size) {
        var pageable = PageRequests.of(page, size, "createdAt", "desc", Set.of("createdAt", "joiningDate"), "createdAt");
        return PageResponse.from(placements.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("candidate").get("fullName")), like),
                        cb.like(cb.lower(root.get("company").get("companyName")), like),
                        cb.like(cb.lower(root.get("designation")), like)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("placementStatus"), status));
            if (companyId != null) predicates.add(cb.equal(root.get("company").get("id"), companyId));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toPlacement));
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkforceDtos.EmployeeResponse> employees(String q, EmployeeStatus status, UUID companyId, int page, int size, String sort, String direction) {
        var pageable = PageRequests.of(page, size, sort, direction, Set.of("fullName", "joiningDate", "employeeCode", "createdAt"), "fullName");
        return PageResponse.from(employees.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("employeeCode")), like),
                        cb.like(cb.lower(root.get("designation")), like)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (companyId != null) predicates.add(cb.equal(root.get("company").get("id"), companyId));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::toEmployee));
    }

    @Transactional(readOnly = true)
    public WorkforceDtos.EmployeeProfile employee(UUID id) {
        Employee employee = requireEmployee(id);
        return new WorkforceDtos.EmployeeProfile(
                toEmployee(employee),
                recruitment.candidate(employee.getCandidate().getId()),
                placements.findByEmployeeIdOrderByCreatedAtDesc(id).stream().map(this::toPlacement).toList(),
                employeeHistory.findByEmployeeIdOrderByChangedAtAsc(id).stream().map(event -> new WorkforceDtos.HistoryItem(
                        event.getOldStatus() == null ? null : event.getOldStatus().name(),
                        event.getNewStatus().name(),
                        event.getChangedBy() == null ? "System" : event.getChangedBy().getFullName(),
                        event.getChangedAt(), event.getReason(), event.getNote()
                )).toList(),
                hire(employee)
        );
    }

    private WorkforceDtos.HireInfo hire(Employee employee) {
        JobApplication application = applications.findByCandidateIdOrderByCreatedAtDesc(employee.getCandidate().getId()).stream().findFirst().orElse(null);
        StoredDocument resume = documents.list("EMPLOYEE", employee.getId()).stream()
                .filter(document -> "RESUME".equals(document.getDocumentType()))
                .findFirst()
                .orElse(null);
        if (resume == null && application != null) {
            resume = documents.list("APPLICATION", application.getId()).stream()
                    .filter(document -> "RESUME".equals(document.getDocumentType()))
                    .findFirst()
                    .orElse(null);
        }
        return new WorkforceDtos.HireInfo(
                application == null ? null : application.getJob().getTitle(),
                application == null ? null : application.getJob().getJobCode(),
                application == null ? null : application.getApplicationNumber(),
                application == null ? null : application.getStatus().name(),
                resume == null ? null : resume.getId(),
                resume == null ? null : resume.getFileName()
        );
    }

    @Transactional
    public WorkforceDtos.EmployeeResponse createEmployee(WorkforceDtos.EmployeeCreate request) {
        String email = request.email().trim().toLowerCase();
        if (candidates.findByEmailIgnoreCase(email).isPresent()) {
            throw new ConflictException("An employee with this email already exists.");
        }
        Company company = companies.require(request.companyId());
        Candidate candidate = new Candidate();
        candidate.setFullName(request.fullName().trim());
        candidate.setMobile(request.mobile().trim());
        candidate.setEmail(email);
        candidate.setAddress(request.address());
        candidate.setCity(request.city());
        candidate.setCountry("India");
        candidates.save(candidate);
        Employee employee = new Employee();
        employee.setEmployeeCode(codes.next("employee_code_seq", "NSS-EMP-", 5));
        employee.setCandidate(candidate);
        employee.setFullName(candidate.getFullName());
        employee.setCompany(company);
        employee.setDesignation(request.designation());
        employee.setDepartment(request.department());
        employee.setJoiningDate(request.joiningDate());
        employee.setEmploymentType(request.employmentType() == null ? com.narayan.staffing.domain.EmploymentType.CONTRACT : request.employmentType());
        employee.setWorkLocation(request.workLocation());
        employee.setReportingManager(request.reportingManager());
        employee.setNotes(request.notes());
        employee.setStatus(EmployeeStatus.ACTIVE);
        employees.save(employee);
        writeEmployeeStatus(employee, null, EmployeeStatus.ACTIVE, "Added from the employee desk", null);
        auditService.log("EMPLOYEE_CREATED", "EMPLOYEE", employee.getId().toString(), Map.of("companyId", company.getId().toString()));
        return toEmployee(employee);
    }

    @Transactional
    public WorkforceDtos.EmployeeResponse updateEmployee(UUID id, WorkforceDtos.EmployeeUpdate request) {
        Employee employee = requireEmployee(id);
        Candidate candidate = employee.getCandidate();
        String email = request.email().trim().toLowerCase();
        candidates.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(candidate.getId())).ifPresent(existing -> {
            throw new ConflictException("An employee with this email already exists.");
        });
        candidate.setFullName(request.fullName().trim());
        candidate.setMobile(request.mobile().trim());
        candidate.setEmail(email);
        candidate.setAddress(blankToNull(request.address()));
        candidate.setCity(blankToNull(request.city()));
        employee.setFullName(candidate.getFullName());
        employee.setCompany(request.companyId() == null ? null : companies.require(request.companyId()));
        employee.setDesignation(blankToNull(request.designation()));
        employee.setDepartment(blankToNull(request.department()));
        employee.setJoiningDate(request.joiningDate());
        employee.setEmploymentType(request.employmentType());
        employee.setWorkLocation(blankToNull(request.workLocation()));
        employee.setReportingManager(blankToNull(request.reportingManager()));
        employee.setNotes(blankToNull(request.notes()));
        auditService.log("EMPLOYEE_UPDATED", "EMPLOYEE", id.toString(), Map.of("name", employee.getFullName()));
        return toEmployee(employee);
    }

    @Transactional
    public void updatePhoto(UUID id, String path) {
        Employee employee = requireEmployee(id);
        employee.setPhotoPath(path);
        employee.getCandidate().setPhotoPath(path);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public WorkforceDtos.EmployeeResponse updateBank(UUID id, WorkforceDtos.BankUpdate request) {
        Employee employee = requireEmployee(id);
        employee.setBankAccountHolder(request.accountHolder().trim());
        employee.setBankName(request.bankName().trim());
        employee.setBankAccountEncrypted(crypto.encrypt(request.accountNumber().replaceAll("\\s+", "")));
        employee.setBankIfsc(request.ifsc().trim().toUpperCase());
        employee.setBankBranch(request.branch());
        auditService.log("BANK_DETAILS_UPDATED", "EMPLOYEE", id.toString(), Map.of());
        return toEmployee(employee);
    }

    @Transactional
    public WorkforceDtos.EmployeeResponse changeEmployeeStatus(UUID id, EmployeeStatus status, String reason, String note) {
        Employee employee = requireEmployee(id);
        EmployeeStatus previous = employee.getStatus();
        employee.setStatus(status);
        writeEmployeeStatus(employee, previous, status, reason, note);
        auditService.log("EMPLOYEE_STATUS", "EMPLOYEE", id.toString(), Map.of("from", previous.name(), "to", status.name()));
        return toEmployee(employee);
    }

    public Employee requireEmployee(UUID id) {
        return employees.findById(id).orElseThrow(() -> new NotFoundException("Employee not found"));
    }

    private void syncEmployee(Placement placement) {
        Employee employee = placement.getEmployee();
        if (employee == null) return;
        PlacementStatus status = placement.getPlacementStatus();
        if (status == PlacementStatus.JOINED || status == PlacementStatus.ACTIVE || status == PlacementStatus.ACCEPTED || status == PlacementStatus.JOINING_PENDING) {
            employee.setCompany(placement.getCompany());
            employee.setDesignation(placement.getDesignation());
            employee.setDepartment(placement.getDepartment());
            employee.setJoiningDate(placement.getJoiningDate());
            employee.setEmploymentType(placement.getEmploymentType());
            employee.setWorkLocation(placement.getWorkLocation());
            employee.setReportingManager(placement.getReportingManager());
            if (employee.getStatus() != EmployeeStatus.ACTIVE) {
                EmployeeStatus previous = employee.getStatus();
                employee.setStatus(EmployeeStatus.ACTIVE);
                writeEmployeeStatus(employee, previous, EmployeeStatus.ACTIVE, "Placement " + status.name(), null);
            }
        }
        if (status == PlacementStatus.EXITED) {
            EmployeeStatus previous = employee.getStatus();
            employee.setStatus(EmployeeStatus.RESIGNED);
            writeEmployeeStatus(employee, previous, EmployeeStatus.RESIGNED, "Placement exited", null);
        }
        JobApplication application = placement.getApplication();
        if (application != null && (status == PlacementStatus.JOINED || status == PlacementStatus.ACTIVE)) {
            if (com.narayan.staffing.recruitment.ApplicationWorkflow.canTransition(application.getStatus(), ApplicationStatus.JOINED)) {
                recruitment.move(application, ApplicationStatus.JOINED, null, "Placement joined");
            }
        }
    }

    private void applyPlacement(Placement placement, WorkforceDtos.PlacementRequest request) {
        placement.setDesignation(request.designation());
        placement.setDepartment(request.department());
        placement.setJoiningDate(request.joiningDate() == null ? LocalDate.now() : request.joiningDate());
        placement.setEmploymentType(request.employmentType());
        placement.setWorkLocation(request.workLocation());
        placement.setReportingManager(request.reportingManager());
        placement.setAgreedSalary(request.agreedSalary());
        placement.setBillingSalary(request.billingSalary());
        placement.setPayrollType(request.payrollType());
        placement.setPlacementStatus(request.placementStatus());
        placement.setOfferDate(request.offerDate() == null ? LocalDate.now() : request.offerDate());
        placement.setJoiningStatus(request.joiningStatus());
        placement.setRemarks(request.remarks());
    }

    private void writePlacementStatus(Placement placement, PlacementStatus from, PlacementStatus to, String reason, String note) {
        PlacementStatusEvent event = new PlacementStatusEvent();
        event.setPlacement(placement);
        event.setOldStatus(from);
        event.setNewStatus(to);
        event.setReason(reason);
        event.setNote(note);
        event.setChangedAt(Instant.now());
        if (SecurityUtils.currentUserId() != null) event.setChangedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        placementHistory.save(event);
    }

    private void writeEmployeeStatus(Employee employee, EmployeeStatus from, EmployeeStatus to, String reason, String note) {
        EmployeeStatusEvent event = new EmployeeStatusEvent();
        event.setEmployee(employee);
        event.setOldStatus(from);
        event.setNewStatus(to);
        event.setReason(reason);
        event.setNote(note);
        event.setChangedAt(Instant.now());
        if (SecurityUtils.currentUserId() != null) event.setChangedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        employeeHistory.save(event);
    }

    private WorkforceDtos.PlacementResponse toPlacement(Placement placement) {
        return new WorkforceDtos.PlacementResponse(
                placement.getId(), placement.getCandidate().getId(), placement.getCandidate().getFullName(),
                placement.getCompany().getId(), placement.getCompany().getCompanyName(),
                placement.getJob() == null ? null : placement.getJob().getId(),
                placement.getJob() == null ? null : placement.getJob().getTitle(),
                placement.getEmployee() == null ? null : placement.getEmployee().getId(),
                placement.getEmployee() == null ? null : placement.getEmployee().getEmployeeCode(),
                placement.getApplication() == null ? null : placement.getApplication().getId(),
                placement.getDesignation(), placement.getDepartment(), placement.getJoiningDate(), placement.getEmploymentType(),
                placement.getWorkLocation(), placement.getReportingManager(), placement.getAgreedSalary(), placement.getBillingSalary(),
                placement.getPayrollType(), placement.getPlacementStatus(), placement.getOfferDate(), placement.getJoiningStatus(),
                placement.getRemarks(), placement.getCreatedAt()
        );
    }

    private WorkforceDtos.EmployeeResponse toEmployee(Employee employee) {
        boolean reveal = SecurityUtils.hasAuthority("SENSITIVE_READ");
        String account = null;
        if (employee.getBankAccountEncrypted() != null) {
            try {
                String plain = crypto.decrypt(employee.getBankAccountEncrypted());
                account = reveal ? plain : SensitiveDataMasker.maskAccount(plain);
            } catch (Exception ignored) {
                account = null;
            }
        }
        var structure = salaryStructures.findByEmployeeIdOrderByEffectiveDateDesc(employee.getId()).stream().findFirst().orElse(null);
        return new WorkforceDtos.EmployeeResponse(
                employee.getId(), employee.getEmployeeCode(), employee.getCandidate().getId(),
                employee.getCompany() == null ? null : employee.getCompany().getId(),
                employee.getCompany() == null ? null : employee.getCompany().getCompanyName(),
                employee.getFullName(), employee.getDesignation(), employee.getDepartment(), employee.getJoiningDate(),
                employee.getEmploymentType(), employee.getWorkLocation(), employee.getReportingManager(), employee.getStatus(),
                employee.getPhotoPath(), employee.getBankAccountHolder(), employee.getBankName(), account, employee.getBankIfsc(),
                employee.getBankBranch(), employee.getNotes(), structure == null ? null : structure.getNetAmount(), employee.getCreatedAt()
        );
    }
}
