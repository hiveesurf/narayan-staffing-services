package com.narayan.staffing.company;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.CodeGenerator;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.common.SensitiveDataMasker;
import com.narayan.staffing.domain.CompanyStatus;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.domain.PlacementStatus;
import com.narayan.staffing.job.JobRepository;
import com.narayan.staffing.payroll.PayrollRunRepository;
import com.narayan.staffing.workforce.EmployeeRepository;
import com.narayan.staffing.workforce.PlacementRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companies;
    private final JobRepository jobs;
    private final EmployeeRepository employees;
    private final PlacementRepository placements;
    private final PayrollRunRepository payrollRuns;
    private final CodeGenerator codes;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<CompanyDtos.CompanyResponse> list(String q, CompanyStatus status, String city, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequests.of(page, size, sort, direction, Set.of("companyName", "city", "createdAt", "status"), "companyName");
        Specification<Company> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("companyName")), like),
                        cb.like(cb.lower(root.get("companyCode")), like),
                        cb.like(cb.lower(root.get("city")), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (city != null && !city.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), city.toLowerCase()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(companies.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public CompanyDtos.CompanyResponse get(java.util.UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public CompanyDtos.CompanyProfile profile(java.util.UUID id) {
        Company company = require(id);
        BigDecimal net = payrollRuns.findByCompanyId(id).stream()
                .map(run -> run.getTotalNet() == null ? BigDecimal.ZERO : run.getTotalNet())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CompanyDtos.NamedCount> recent = jobs.findAll((root, query, cb) -> cb.equal(root.get("company").get("id"), id))
                .stream().limit(8)
                .map(job -> new CompanyDtos.NamedCount(job.getId(), job.getTitle(), job.getStatus().name()))
                .toList();
        return new CompanyDtos.CompanyProfile(
                toResponse(company),
                jobs.countByCompanyIdAndStatus(id, JobStatus.PUBLISHED),
                employees.countByCompanyIdAndStatus(id, EmployeeStatus.ACTIVE),
                placements.countByCompanyIdAndPlacementStatusIn(id, List.of(PlacementStatus.JOINED, PlacementStatus.ACTIVE, PlacementStatus.OFFERED)),
                net,
                recent
        );
    }

    @Transactional
    public CompanyDtos.CompanyResponse create(CompanyDtos.CompanyRequest request) {
        Company company = new Company();
        company.setCompanyCode(codes.next("company_code_seq", "NSS-CO-", 5));
        apply(company, request);
        companies.save(company);
        auditService.log("COMPANY_CREATED", "COMPANY", company.getId().toString(), Map.of("name", company.getCompanyName()));
        return toResponse(company);
    }

    @Transactional
    public CompanyDtos.CompanyResponse update(java.util.UUID id, CompanyDtos.CompanyRequest request) {
        Company company = require(id);
        apply(company, request);
        auditService.log("COMPANY_UPDATED", "COMPANY", company.getId().toString(), Map.of("name", company.getCompanyName()));
        return toResponse(company);
    }

    @Transactional
    public void delete(java.util.UUID id) {
        Company company = require(id);
        if (jobs.countByCompanyIdAndStatus(id, JobStatus.PUBLISHED) > 0 || employees.countByCompanyIdAndStatus(id, EmployeeStatus.ACTIVE) > 0) {
            throw new ConflictException("This company still has open jobs or active employees. Mark it inactive instead.");
        }
        companies.delete(company);
        auditService.log("COMPANY_DELETED", "COMPANY", id.toString(), Map.of());
    }

    @Transactional
    public CompanyDtos.CompanyResponse setStatus(java.util.UUID id, CompanyStatus status) {
        Company company = require(id);
        company.setStatus(status);
        auditService.log("COMPANY_STATUS", "COMPANY", id.toString(), Map.of("status", status.name()));
        return toResponse(company);
    }

    @Transactional
    public void updateLogo(java.util.UUID id, String path) {
        require(id).setLogoPath(path);
    }

    @Transactional
    public CompanyDtos.CompanyResponse addCover(java.util.UUID id, String path) {
        Company company = require(id);
        List<String> covers = new ArrayList<>(company.getCoverPaths() == null ? List.of() : company.getCoverPaths());
        if (covers.size() >= 4) {
            throw new ConflictException("A company can keep up to 4 cover photos.");
        }
        covers.add(path);
        company.setCoverPaths(covers);
        return toResponse(company);
    }

    @Transactional
    public CompanyDtos.CompanyResponse removeCover(java.util.UUID id, String path) {
        Company company = require(id);
        List<String> covers = new ArrayList<>(company.getCoverPaths() == null ? List.of() : company.getCoverPaths());
        covers.remove(path);
        company.setCoverPaths(covers);
        return toResponse(company);
    }

    public Company require(java.util.UUID id) {
        return companies.findById(id).orElseThrow(() -> new NotFoundException("Company not found"));
    }

    public List<Company> allActive() {
        return companies.findAll((root, query, cb) -> cb.equal(root.get("status"), CompanyStatus.ACTIVE));
    }

    private void apply(Company company, CompanyDtos.CompanyRequest request) {
        company.setCompanyName(request.companyName().trim());
        company.setLegalName(request.legalName());
        company.setIndustry(request.industry());
        company.setCompanyType(request.companyType());
        company.setWebsite(request.website());
        company.setEmail(request.email());
        company.setPhone(request.phone());
        company.setAddress(request.address());
        company.setCity(request.city());
        company.setState(request.state());
        company.setCountry(request.country() == null || request.country().isBlank() ? "India" : request.country());
        company.setPincode(request.pincode());
        company.setGstNumber(request.gstNumber());
        if (request.pan() != null && !request.pan().contains("X")) {
            company.setPan(request.pan().trim().toUpperCase());
        }
        company.setContactPerson(request.contactPerson());
        company.setContactPersonDesignation(request.contactPersonDesignation());
        company.setContactPersonPhone(request.contactPersonPhone());
        company.setContactPersonEmail(request.contactPersonEmail());
        company.setBillingAddress(request.billingAddress());
        company.setStatus(request.status());
        company.setNotes(request.notes());
    }

    private CompanyDtos.CompanyResponse toResponse(Company company) {
        boolean reveal = SecurityUtils.hasAuthority("SENSITIVE_READ") || SecurityUtils.hasAuthority("COMPANY_WRITE");
        String pan = reveal ? company.getPan() : SensitiveDataMasker.maskPan(company.getPan());
        return new CompanyDtos.CompanyResponse(
                company.getId(), company.getCompanyCode(), company.getCompanyName(), company.getLegalName(), company.getIndustry(),
                company.getCompanyType(), company.getWebsite(), company.getEmail(), company.getPhone(), company.getAddress(),
                company.getCity(), company.getState(), company.getCountry(), company.getPincode(), company.getGstNumber(), pan,
                company.getContactPerson(), company.getContactPersonDesignation(), company.getContactPersonPhone(),
                company.getContactPersonEmail(), company.getBillingAddress(), company.getStatus(), company.getNotes(),
                company.getLogoPath(), company.getCoverPaths() == null ? List.of() : List.copyOf(company.getCoverPaths()),
                company.getCreatedAt(), company.getUpdatedAt()
        );
    }
}
