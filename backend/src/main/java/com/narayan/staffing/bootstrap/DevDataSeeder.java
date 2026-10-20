package com.narayan.staffing.bootstrap;

import com.narayan.staffing.candidate.Candidate;
import com.narayan.staffing.candidate.CandidateRepository;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.company.CompanyRepository;
import com.narayan.staffing.config.AppProperties;
import com.narayan.staffing.domain.CompanyStatus;
import com.narayan.staffing.domain.EmploymentType;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.domain.SalaryType;
import com.narayan.staffing.identity.AuthService;
import com.narayan.staffing.identity.RoleRepository;
import com.narayan.staffing.identity.UserRepository;
import com.narayan.staffing.job.Job;
import com.narayan.staffing.job.JobCategoryRepository;
import com.narayan.staffing.job.JobRepository;
import com.narayan.staffing.recruitment.JobApplication;
import com.narayan.staffing.recruitment.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final AppProperties properties;
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuthService authService;
    private final CompanyRepository companies;
    private final JobCategoryRepository categories;
    private final JobRepository jobs;
    private final CandidateRepository candidates;
    private final JobApplicationRepository applications;

    @Override
    @Transactional
    public void run(String... args) {
        var bootstrap = properties.getBootstrap();
        if (!bootstrap.isSeedSampleData() || jobs.count() > 0) {
            return;
        }
        bootstrap.getSampleUsers().forEach(sample ->
                authService.ensureUser(sample.getEmail(), sample.getPassword(), sample.getName(), sample.getRole(), roles));
        Company auto = company("Deccan Auto Components", "Automotive", "Chakan", "Plot 14, MIDC Phase 2", "410501", "/media/site-work.jpg", CompanyStatus.ACTIVE);
        Company warehouse = company("Western Warehousing", "Logistics", "Pune", "Survey 88, Talegaon Road", "411033", "/media/hero-workforce.jpg", CompanyStatus.ACTIVE);
        company("Shakti Facilities", "Facility Management", "Pimpri", "B-2, Pimpri Industrial Estate", "411018", "/media/team-desk.jpg", CompanyStatus.PROSPECT);
        var manufacturing = categories.findBySlug("manufacturing").orElseThrow();
        var engineering = categories.findBySlug("engineering").orElseThrow();
        var facility = categories.findBySlug("facility-management").orElseThrow();
        Job technician = job("CNC Technician", "cnc-technician-chakan", auto, manufacturing, "Chakan", "Production", new BigDecimal("2"), new BigDecimal("6"), new BigDecimal("18000"), new BigDecimal("28000"), 12);
        job("Quality Inspector", "quality-inspector-chakan", auto, engineering, "Chakan", "Quality", new BigDecimal("3"), new BigDecimal("8"), new BigDecimal("22000"), new BigDecimal("35000"), 4);
        job("Warehouse Associate", "warehouse-associate-pune", warehouse, facility, "Pune", "Operations", BigDecimal.ZERO, new BigDecimal("3"), new BigDecimal("15000"), new BigDecimal("22000"), 20);
        Candidate candidate = new Candidate();
        candidate.setFullName("Ramesh Kale");
        candidate.setMobile("9876501234");
        candidate.setEmail("ramesh.kale@example.com");
        candidate.setCity("Chakan");
        candidate.setState("Maharashtra");
        candidate.setHighestQualification("ITI");
        candidate.setTotalExperience(new BigDecimal("4"));
        candidate.setExpectedSalary(new BigDecimal("24000"));
        candidate.setCurrentJobTitle("CNC Operator");
        candidates.save(candidate);
        JobApplication application = new JobApplication();
        application.setApplicationNumber("NSS-APP-SEED-00001");
        application.setCandidate(candidate);
        application.setJob(technician);
        application.setConsent(true);
        applications.save(application);
        log.info("Sample companies, jobs and one application were created.");
    }

    private Company company(String name, String industry, String city, String address, String pincode, String cover, CompanyStatus status) {
        Company company = new Company();
        company.setCompanyCode("NSS-CO-SEED-" + (companies.count() + 1));
        company.setCompanyName(name);
        company.setIndustry(industry);
        company.setCity(city);
        company.setAddress(address);
        company.setPincode(pincode);
        company.setCoverPaths(new ArrayList<>(List.of(cover)));
        company.setState("Maharashtra");
        company.setCountry("India");
        company.setStatus(status);
        company.setCompanyType("Private Limited");
        return companies.save(company);
    }

    private Job job(String title, String slug, Company company, com.narayan.staffing.job.JobCategory category, String location, String department,
                    BigDecimal minExp, BigDecimal maxExp, BigDecimal salaryMin, BigDecimal salaryMax, int vacancies) {
        Job job = new Job();
        job.setTitle(title);
        job.setSlug(slug);
        job.setJobCode("NSS-JOB-SEED-" + (jobs.count() + 1));
        job.setCompany(company);
        job.setCategory(category);
        job.setLocation(location);
        job.setDepartment(department);
        job.setEmploymentType(EmploymentType.CONTRACT);
        job.setExperienceMin(minExp);
        job.setExperienceMax(maxExp);
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setSalaryType(SalaryType.MONTHLY);
        job.setVacancies(vacancies);
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishDate(Instant.now());
        job.setJobDescription("Deployed role for " + company.getCompanyName() + " at the " + location + " site.");
        job.setResponsibilities("Work the assigned shift, follow site safety rules, and report to the supervisor.");
        job.setRequiredSkills("Relevant trade experience, punctuality, ability to work in a plant environment");
        job.setBenefits("Statutory deductions as configured, site transport where applicable");
        job.setDeadline(LocalDate.now().plusMonths(2));
        job.setEducation("ITI or equivalent experience");
        return jobs.save(job);
    }
}
