package com.narayan.staffing.job;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.CodeGenerator;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SlugUtils;
import com.narayan.staffing.company.CompanyService;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.recruitment.JobApplicationRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobs;
    private final JobCategoryRepository categories;
    private final JobApplicationRepository applications;
    private final CompanyService companies;
    private final CodeGenerator codes;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<JobDtos.JobResponse> search(String q, String category, String location, BigDecimal salaryMin, BigDecimal salaryMax,
                                                     EmploymentType employmentType, String experienceLevel, JobStatus status, Boolean featured,
                                                     UUID companyId, boolean publicOnly, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequests.of(page, size, sort, direction, Set.of("publishDate", "createdAt", "title", "salaryMin"), publicOnly ? "publishDate" : "createdAt");
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (publicOnly) {
                predicates.add(cb.equal(root.get("status"), JobStatus.PUBLISHED));
                predicates.add(cb.or(cb.isNull(root.get("deadline")), cb.greaterThanOrEqualTo(root.get("deadline"), java.time.LocalDate.now())));
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("location")), like),
                        cb.like(cb.lower(root.get("requiredSkills")), like),
                        cb.like(cb.lower(root.get("jobCode")), like)
                ));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category").get("slug"), category));
            }
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
            }
            if (salaryMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), salaryMin));
            }
            if (salaryMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), salaryMax));
            }
            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            experience(predicates, cb, root, experienceLevel);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(jobs.findAll(spec, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public JobDtos.JobResponse get(UUID id) {
        return toResponse(require(id));
    }

    @Transactional(readOnly = true)
    public JobDtos.JobResponse getPublishedBySlug(String slug) {
        Job job = jobs.findBySlug(slug).filter(item -> item.getStatus() == JobStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Job not found"));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobDtos.JobResponse> related(String slug) {
        Job job = jobs.findBySlug(slug).orElseThrow(() -> new NotFoundException("Job not found"));
        return jobs.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("status"), JobStatus.PUBLISHED),
                cb.notEqual(root.get("id"), job.getId()),
                job.getCategory() == null ? cb.conjunction() : cb.equal(root.get("category").get("id"), job.getCategory().getId())
        )).stream().limit(3).map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<String> locations() {
        return jobs.findAll((root, query, cb) -> cb.equal(root.get("status"), JobStatus.PUBLISHED)).stream()
                .map(Job::getLocation).distinct().sorted().toList();
    }

    @Transactional
    public JobDtos.JobResponse create(JobDtos.JobRequest request) {
        Job job = new Job();
        job.setJobCode(codes.next("job_code_seq", "NSS-JOB-", 5));
        job.setStatus(JobStatus.DRAFT);
        apply(job, request);
        job.setSlug(uniqueSlug(request.title() + "-" + request.location(), null));
        jobs.save(job);
        auditService.log("JOB_CREATED", "JOB", job.getId().toString(), Map.of("code", job.getJobCode()));
        return toResponse(job);
    }

    @Transactional
    public JobDtos.JobResponse update(UUID id, JobDtos.JobRequest request) {
        Job job = require(id);
        if (job.getStatus() == JobStatus.PUBLISHED) {
            throw new BusinessException("Unpublish this job before editing it.");
        }
        apply(job, request);
        auditService.log("JOB_EDITED", "JOB", job.getId().toString(), Map.of("code", job.getJobCode()));
        return toResponse(job);
    }

    @Transactional
    public JobDtos.JobResponse duplicate(UUID id) {
        Job source = require(id);
        Job copy = new Job();
        copy.setJobCode(codes.next("job_code_seq", "NSS-JOB-", 5));
        copy.setTitle(source.getTitle());
        copy.setCategory(source.getCategory());
        copy.setCompany(source.getCompany());
        copy.setLocation(source.getLocation());
        copy.setDepartment(source.getDepartment());
        copy.setEmploymentType(source.getEmploymentType());
        copy.setExperienceMin(source.getExperienceMin());
        copy.setExperienceMax(source.getExperienceMax());
        copy.setEducation(source.getEducation());
        copy.setSalaryMin(source.getSalaryMin());
        copy.setSalaryMax(source.getSalaryMax());
        copy.setSalaryType(source.getSalaryType());
        copy.setVacancies(source.getVacancies());
        copy.setJobDescription(source.getJobDescription());
        copy.setResponsibilities(source.getResponsibilities());
        copy.setRequiredSkills(source.getRequiredSkills());
        copy.setPreferredSkills(source.getPreferredSkills());
        copy.setBenefits(source.getBenefits());
        copy.setDeadline(source.getDeadline());
        copy.setFeatured(false);
        copy.setStatus(JobStatus.DRAFT);
        copy.setSlug(uniqueSlug(source.getTitle() + "-" + source.getLocation(), null));
        jobs.save(copy);
        return toResponse(copy);
    }

    @Transactional
    public JobDtos.JobResponse setStatus(UUID id, JobStatus status) {
        Job job = require(id);
        if (status == JobStatus.PUBLISHED) {
            job.setPublishDate(Instant.now());
        }
        job.setStatus(status);
        auditService.log("JOB_" + status.name(), "JOB", job.getId().toString(), Map.of());
        return toResponse(job);
    }

    @Transactional
    public void delete(UUID id) {
        Job job = require(id);
        if (applications.countByJobId(id) > 0) {
            throw new ConflictException("This job has applications. Archive it instead of deleting it.");
        }
        jobs.delete(job);
        auditService.log("JOB_DELETED", "JOB", id.toString(), Map.of());
    }

    @Transactional(readOnly = true)
    public List<JobDtos.CategoryResponse> categories(boolean activeOnly) {
        return categories.findAll().stream()
                .filter(category -> !activeOnly || category.isActive())
                .sorted(java.util.Comparator.comparingInt(JobCategory::getDisplayOrder))
                .map(this::toCategory)
                .toList();
    }

    @Transactional
    public JobDtos.CategoryResponse saveCategory(UUID id, JobDtos.CategoryRequest request) {
        JobCategory category = id == null ? new JobCategory() : categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        String slug = SlugUtils.slugify(request.name());
        if ((id == null && categories.existsBySlug(slug)) || (id != null && categories.findBySlug(slug).filter(existing -> !existing.getId().equals(id)).isPresent())) {
            throw new ConflictException("A category with that name already exists.");
        }
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setIcon(request.icon());
        category.setActive(request.active());
        category.setDisplayOrder(request.displayOrder());
        return toCategory(categories.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        JobCategory category = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        boolean used = jobs.findAll((root, query, cb) -> cb.equal(root.get("category").get("id"), id)).stream().findAny().isPresent();
        if (used) {
            throw new ConflictException("Jobs still use this category.");
        }
        categories.delete(category);
    }

    public Job require(UUID id) {
        return jobs.findById(id).orElseThrow(() -> new NotFoundException("Job not found"));
    }

    public Job requirePublishedSlug(String slug) {
        return jobs.findBySlug(slug).filter(job -> job.getStatus() == JobStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Job not found"));
    }

    private void apply(Job job, JobDtos.JobRequest request) {
        if (request.vacancies() < 1) {
            throw new BusinessException("Vacancies must be at least 1.");
        }
        job.setTitle(request.title().trim());
        job.setCategory(request.categoryId() == null ? null : categories.findById(request.categoryId()).orElseThrow(() -> new NotFoundException("Category not found")));
        job.setCompany(companies.require(request.companyId()));
        job.setLocation(request.location().trim());
        job.setDepartment(request.department());
        job.setEmploymentType(request.employmentType());
        job.setExperienceMin(request.experienceMin());
        job.setExperienceMax(request.experienceMax());
        job.setEducation(request.education());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setSalaryType(request.salaryType());
        job.setVacancies(request.vacancies());
        job.setJobDescription(request.jobDescription());
        job.setResponsibilities(request.responsibilities());
        job.setRequiredSkills(request.requiredSkills());
        job.setPreferredSkills(request.preferredSkills());
        job.setBenefits(request.benefits());
        job.setDeadline(request.deadline());
        job.setFeatured(request.featured());
    }

    private String uniqueSlug(String source, UUID currentId) {
        String base = SlugUtils.slugify(source);
        String slug = base;
        int i = 2;
        while (jobs.findBySlug(slug).filter(existing -> currentId == null || !existing.getId().equals(currentId)).isPresent()) {
            slug = base + "-" + i++;
        }
        return slug;
    }

    private void experience(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb, jakarta.persistence.criteria.Root<Job> root, String level) {
        if (level == null || level.isBlank()) {
            return;
        }
        BigDecimal min = switch (level.toUpperCase()) {
            case "FRESHER" -> BigDecimal.ZERO;
            case "JUNIOR" -> BigDecimal.ONE;
            case "MID" -> new BigDecimal("3");
            case "SENIOR" -> new BigDecimal("6");
            default -> null;
        };
        BigDecimal max = switch (level.toUpperCase()) {
            case "FRESHER" -> BigDecimal.ONE;
            case "JUNIOR" -> new BigDecimal("3");
            case "MID" -> new BigDecimal("6");
            case "SENIOR" -> new BigDecimal("40");
            default -> null;
        };
        if (min == null) {
            return;
        }
        predicates.add(cb.and(
                cb.or(cb.isNull(root.get("experienceMax")), cb.greaterThanOrEqualTo(root.get("experienceMax"), min)),
                cb.or(cb.isNull(root.get("experienceMin")), cb.lessThanOrEqualTo(root.get("experienceMin"), max))
        ));
    }

    private JobDtos.JobResponse toResponse(Job job) {
        return new JobDtos.JobResponse(
                job.getId(), job.getTitle(), job.getSlug(), job.getJobCode(),
                job.getCategory() == null ? null : job.getCategory().getId(),
                job.getCategory() == null ? null : job.getCategory().getName(),
                job.getCompany().getId(), job.getCompany().getCompanyName(),
                job.getLocation(), job.getDepartment(), job.getEmploymentType(),
                job.getExperienceMin(), job.getExperienceMax(), job.getEducation(),
                job.getSalaryMin(), job.getSalaryMax(), job.getSalaryType(), job.getVacancies(),
                job.getJobDescription(), job.getResponsibilities(), job.getRequiredSkills(), job.getPreferredSkills(),
                job.getBenefits(), job.getDeadline(), job.getStatus(), job.isFeatured(), job.getPublishDate(),
                job.getCreatedAt(), applications.countByJobId(job.getId())
        );
    }

    private JobDtos.CategoryResponse toCategory(JobCategory category) {
        return new JobDtos.CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.getIcon(), category.isActive(), category.getDisplayOrder(), category.getCreatedAt(), category.getUpdatedAt());
    }
}
