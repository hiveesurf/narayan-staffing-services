package com.narayan.staffing.job;

import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.JobStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping("/api/jobs")
    @PreAuthorize("hasAuthority('JOB_READ')")
    public PageResponse<JobDtos.JobResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return jobService.search(q, category, location, salaryMin, salaryMax, employmentType, experienceLevel, status, featured, companyId, false, page, size, sort, direction);
    }

    @GetMapping("/api/jobs/{id}")
    @PreAuthorize("hasAuthority('JOB_READ')")
    public JobDtos.JobResponse get(@PathVariable UUID id) {
        return jobService.get(id);
    }

    @PostMapping("/api/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse create(@Valid @RequestBody JobDtos.JobRequest request) {
        return jobService.create(request);
    }

    @PutMapping("/api/jobs/{id}")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse update(@PathVariable UUID id, @Valid @RequestBody JobDtos.JobRequest request) {
        return jobService.update(id, request);
    }

    @PostMapping("/api/jobs/{id}/duplicate")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse duplicate(@PathVariable UUID id) {
        return jobService.duplicate(id);
    }

    @PostMapping("/api/jobs/{id}/publish")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse publish(@PathVariable UUID id) {
        return jobService.setStatus(id, JobStatus.PUBLISHED);
    }

    @PostMapping("/api/jobs/{id}/unpublish")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse unpublish(@PathVariable UUID id) {
        return jobService.setStatus(id, JobStatus.PAUSED);
    }

    @PostMapping("/api/jobs/{id}/close")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse close(@PathVariable UUID id) {
        return jobService.setStatus(id, JobStatus.CLOSED);
    }

    @PostMapping("/api/jobs/{id}/archive")
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public JobDtos.JobResponse archive(@PathVariable UUID id) {
        return jobService.setStatus(id, JobStatus.ARCHIVED);
    }

    @DeleteMapping("/api/jobs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('JOB_WRITE')")
    public void delete(@PathVariable UUID id) {
        jobService.delete(id);
    }

    @GetMapping("/api/categories")
    @PreAuthorize("hasAuthority('JOB_READ') or hasAuthority('CMS_READ')")
    public List<JobDtos.CategoryResponse> categories() {
        return jobService.categories(false);
    }

    @PostMapping("/api/categories")
    @PreAuthorize("hasAuthority('JOB_WRITE') or hasAuthority('CMS_WRITE')")
    public JobDtos.CategoryResponse createCategory(@Valid @RequestBody JobDtos.CategoryRequest request) {
        return jobService.saveCategory(null, request);
    }

    @PutMapping("/api/categories/{id}")
    @PreAuthorize("hasAuthority('JOB_WRITE') or hasAuthority('CMS_WRITE')")
    public JobDtos.CategoryResponse updateCategory(@PathVariable UUID id, @Valid @RequestBody JobDtos.CategoryRequest request) {
        return jobService.saveCategory(id, request);
    }

    @DeleteMapping("/api/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('JOB_WRITE') or hasAuthority('CMS_WRITE')")
    public void deleteCategory(@PathVariable UUID id) {
        jobService.deleteCategory(id);
    }

    @GetMapping("/api/public/jobs")
    public PageResponse<JobDtos.JobResponse> publicJobs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) EmploymentType employmentType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "publishDate") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return jobService.search(q, category, location, salaryMin, salaryMax, employmentType, experienceLevel, null, null, null, true, page, size, sort, direction);
    }

    @GetMapping("/api/public/jobs/locations")
    public List<String> locations() {
        return jobService.locations();
    }

    @GetMapping("/api/public/jobs/{slug}")
    public Map<String, Object> publicJob(@PathVariable String slug) {
        return Map.of("job", jobService.getPublishedBySlug(slug), "related", jobService.related(slug));
    }

    @GetMapping("/api/public/categories")
    public List<JobDtos.CategoryResponse> publicCategories() {
        return jobService.categories(true);
    }
}
