package com.narayan.staffing.dashboard;

import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.CompanyStatus;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.JobStatus;
import com.narayan.staffing.domain.PlacementStatus;
import com.narayan.staffing.job.JobCategoryRepository;
import com.narayan.staffing.payroll.PayrollRunRepository;
import com.narayan.staffing.recruitment.JobApplicationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final JobApplicationRepository applications;
    private final com.narayan.staffing.job.JobRepository jobs;
    private final com.narayan.staffing.workforce.EmployeeRepository employees;
    private final com.narayan.staffing.company.CompanyRepository companies;
    private final com.narayan.staffing.workforce.PlacementRepository placements;
    private final PayrollRunRepository payrollRuns;
    private final JobCategoryRepository categories;
    private final EntityManager entityManager;

    @GetMapping("/api/dashboard")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    public Map<String, Object> dashboard(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        LocalDate startDate = from == null ? LocalDate.now().minusMonths(5).withDayOfMonth(1) : from;
        LocalDate endDate = to == null ? LocalDate.now() : to;
        Instant start = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("openJobs", jobs.countByStatus(JobStatus.PUBLISHED));
        body.put("newApplications", applications.countByCreatedAtBetween(start, end));
        body.put("shortlisted", applications.countByStatus(ApplicationStatus.SHORTLISTED));
        body.put("interviews", applications.countByStatus(ApplicationStatus.INTERVIEW_SCHEDULED) + applications.countByStatus(ApplicationStatus.INTERVIEWED));
        body.put("selected", applications.countByStatus(ApplicationStatus.SELECTED));
        body.put("joined", placements.countByPlacementStatus(PlacementStatus.JOINED) + placements.countByPlacementStatus(PlacementStatus.ACTIVE));
        body.put("employees", employees.countByStatus(EmployeeStatus.ACTIVE));
        body.put("companies", companies.countByStatus(CompanyStatus.ACTIVE));
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (var run : payrollRuns.findAll()) {
            if (run.getPeriodYear() == endDate.getYear() && run.getPeriodMonth() == endDate.getMonthValue()) {
                gross = gross.add(nz(run.getTotalGross()));
                deductions = deductions.add(nz(run.getTotalDeductions()));
                net = net.add(nz(run.getTotalNet()));
            }
        }
        body.put("payroll", Map.of("gross", gross, "deductions", deductions, "net", net));
        body.put("applicationsByMonth", applicationsByMonth(startDate, endDate));
        body.put("applicationsByCategory", applicationsByCategory(start, end));
        body.put("funnel", funnel(start, end));
        body.put("employeesByCompany", employeesByCompany());
        return body;
    }

    @GetMapping("/api/search")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> search(@RequestParam String q) {
        String term = q == null ? "" : q.trim().toLowerCase();
        String like = "%" + term + "%";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pages", pages(term));
        if (term.isBlank()) {
            return body;
        }
        body.put("employees", query("select e.id, e.fullName, coalesce(c.companyName, e.employeeCode) from Employee e left join e.company c where lower(e.fullName) like :q or lower(e.employeeCode) like :q or lower(coalesce(e.designation, '')) like :q or lower(coalesce(c.companyName, '')) like :q", like));
        body.put("candidates", query("select c.id, c.fullName, c.email from Candidate c where lower(c.fullName) like :q or lower(c.email) like :q or c.mobile like :q or lower(coalesce(c.city, '')) like :q", like));
        body.put("companies", query("select c.id, c.companyName, c.companyCode from Company c where lower(c.companyName) like :q or lower(c.companyCode) like :q or lower(coalesce(c.city, '')) like :q", like));
        body.put("jobs", query("select j.id, j.title, concat(j.jobCode, ' · ', coalesce(c.companyName, j.location)) from Job j left join j.company c where lower(j.title) like :q or lower(j.jobCode) like :q or lower(coalesce(j.location, '')) like :q or lower(coalesce(c.companyName, '')) like :q", like));
        body.put("applications", query("select a.id, a.candidate.fullName, concat(a.applicationNumber, ' · ', a.job.title) from JobApplication a where lower(a.applicationNumber) like :q or lower(a.candidate.fullName) like :q or lower(a.job.title) like :q", like));
        body.put("payroll", payrollRuns(term));
        body.put("slips", query("select rec.id, rec.employeeName, concat(c.companyName, ' · ', rec.employeeCode) from PayrollRecord rec join rec.payrollRun run join run.company c where lower(rec.employeeName) like :q or lower(rec.employeeCode) like :q or lower(c.companyName) like :q", like));
        body.put("placements", query("select p.id, p.candidate.fullName, p.company.companyName from Placement p left join p.job j where lower(p.candidate.fullName) like :q or lower(p.company.companyName) like :q or lower(coalesce(j.title, '')) like :q", like));
        body.put("interviews", query("select i.id, i.candidate.fullName, i.job.title from Interview i where lower(i.candidate.fullName) like :q or lower(i.job.title) like :q", like));
        body.put("users", query("select u.id, u.fullName, u.email from User u where lower(u.fullName) like :q or lower(u.email) like :q", like));
        return body;
    }

    private List<Map<String, Object>> pages(String term) {
        List<String[]> pages = List.of(
                new String[]{"/admin/dashboard", "Dashboard", "Overview"},
                new String[]{"/admin/reports", "Reports", "Overview"},
                new String[]{"/admin/jobs", "Jobs", "Hiring"},
                new String[]{"/admin/applications", "Applications", "Hiring"},
                new String[]{"/admin/candidates", "Candidates", "Hiring"},
                new String[]{"/admin/interviews", "Interviews", "Hiring"},
                new String[]{"/admin/companies", "Companies", "Workforce"},
                new String[]{"/admin/placements", "Placements", "Workforce"},
                new String[]{"/admin/employees", "Employees", "Workforce"},
                new String[]{"/admin/payroll", "Payroll", "Payroll"},
                new String[]{"/admin/salary-components", "Salary components", "Payroll"},
                new String[]{"/admin/services", "Services", "Website"},
                new String[]{"/admin/contact-messages", "Messages", "Website"},
                new String[]{"/admin/users", "Users", "Office"},
                new String[]{"/admin/audit-logs", "Audit log", "Office"},
                new String[]{"/admin/settings", "Settings", "Office"}
        );
        if (term.isBlank()) {
            return List.of();
        }
        return pages.stream()
                .filter(page -> page[1].toLowerCase().contains(term) || page[2].toLowerCase().contains(term))
                .map(page -> Map.<String, Object>of("id", page[0], "label", page[1], "sub", page[2]))
                .toList();
    }

    private List<Map<String, Object>> payrollRuns(String term) {
        List<Object[]> rows = entityManager.createQuery(
                "select r.id, c.companyName, r.periodMonth, r.periodYear from PayrollRun r join r.company c order by r.periodYear desc, r.periodMonth desc",
                Object[].class).setMaxResults(36).getResultList();
        List<Map<String, Object>> matches = new ArrayList<>();
        for (Object[] row : rows) {
            int month = ((Number) row[2]).intValue();
            int year = ((Number) row[3]).intValue();
            String company = String.valueOf(row[1]);
            String period = month + "/" + year;
            String text = (company + " " + period + " " + monthName(month) + " " + year + " payroll").toLowerCase();
            if (!text.contains(term)) {
                continue;
            }
            matches.add(Map.of("id", row[0], "label", company, "sub", monthName(month) + " " + year));
            if (matches.size() == 5) {
                break;
            }
        }
        return matches;
    }

    private static String monthName(int month) {
        return java.time.Month.of(month).name().charAt(0) + java.time.Month.of(month).name().substring(1).toLowerCase();
    }

    private List<Map<String, Object>> applicationsByMonth(LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate cursor = start.withDayOfMonth(1);
        while (!cursor.isAfter(end)) {
            Instant from = cursor.atStartOfDay().toInstant(ZoneOffset.UTC);
            Instant to = cursor.plusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            rows.add(Map.of("month", cursor.toString().substring(0, 7), "count", applications.countByCreatedAtBetween(from, to)));
            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    private List<Map<String, Object>> applicationsByCategory(Instant start, Instant end) {
        List<Object[]> rows = entityManager.createQuery(
                "select coalesce(j.category.name, 'Uncategorised'), count(a.id) from JobApplication a join a.job j where a.createdAt >= :start and a.createdAt < :end group by j.category.name",
                Object[].class).setParameter("start", start).setParameter("end", end).getResultList();
        return rows.stream().map(row -> Map.<String, Object>of("category", row[0], "count", row[1])).toList();
    }

    private List<Map<String, Object>> funnel(Instant start, Instant end) {
        long applied = applications.countByCreatedAtBetween(start, end);
        long screening = countStatus(ApplicationStatus.SCREENING, start, end);
        long shortlisted = countStatus(ApplicationStatus.SHORTLISTED, start, end);
        long interviewed = countStatus(ApplicationStatus.INTERVIEWED, start, end) + countStatus(ApplicationStatus.INTERVIEW_SCHEDULED, start, end);
        long selected = countStatus(ApplicationStatus.SELECTED, start, end);
        long joined = countStatus(ApplicationStatus.JOINED, start, end);
        List<Map<String, Object>> stages = new ArrayList<>();
        addStage(stages, "Applications", applied, applied);
        addStage(stages, "Screening", screening, applied);
        addStage(stages, "Shortlisted", shortlisted, applied);
        addStage(stages, "Interviewed", interviewed, applied);
        addStage(stages, "Selected", selected, applied);
        addStage(stages, "Joined", joined, applied);
        return stages;
    }

    private long countStatus(ApplicationStatus status, Instant start, Instant end) {
        return applications.countByStatusAndCreatedAtBetween(status, start, end);
    }

    private void addStage(List<Map<String, Object>> stages, String name, long count, long base) {
        double percent = base == 0 ? 0 : Math.round((count * 1000.0) / base) / 10.0;
        double previous = stages.isEmpty() ? count : ((Number) stages.get(stages.size() - 1).get("count")).longValue();
        double conversion = previous == 0 ? 0 : Math.round((count * 1000.0) / previous) / 10.0;
        stages.add(Map.of("stage", name, "count", count, "percent", percent, "conversion", conversion));
    }

    private List<Map<String, Object>> employeesByCompany() {
        List<Object[]> rows = entityManager.createQuery(
                "select c.companyName, count(e.id) from Employee e join e.company c where e.status = :status group by c.companyName",
                Object[].class).setParameter("status", EmployeeStatus.ACTIVE).getResultList();
        return rows.stream().map(row -> Map.<String, Object>of("company", row[0], "count", row[1])).toList();
    }

    private List<Map<String, Object>> query(String jpql, String like) {
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).setParameter("q", like).setMaxResults(5).getResultList();
        return rows.stream().map(row -> Map.<String, Object>of("id", row[0], "label", row[1], "sub", row[2] == null ? "" : row[2])).toList();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
