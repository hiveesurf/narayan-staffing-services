package com.narayan.staffing.report;

import com.narayan.staffing.domain.ApplicationStatus;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.JobStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final EntityManager entityManager;

    @GetMapping("/api/reports/recruitment")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public List<?> recruitment(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
        Instant start = (from == null ? LocalDate.now().minusMonths(6) : from).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = (to == null ? LocalDate.now().plusDays(1) : to.plusDays(1)).atStartOfDay().toInstant(ZoneOffset.UTC);
        return entityManager.createQuery("""
                select j.title, j.jobCode, count(a.id),
                sum(case when a.status = :shortlisted then 1 else 0 end),
                sum(case when a.status in (:interviewed, :scheduled) then 1 else 0 end),
                sum(case when a.status = :selected then 1 else 0 end),
                sum(case when a.status = :joined then 1 else 0 end)
                from JobApplication a join a.job j
                where a.createdAt >= :start and a.createdAt < :end
                group by j.title, j.jobCode
                """, Object[].class)
                .setParameter("shortlisted", ApplicationStatus.SHORTLISTED)
                .setParameter("interviewed", ApplicationStatus.INTERVIEWED)
                .setParameter("scheduled", ApplicationStatus.INTERVIEW_SCHEDULED)
                .setParameter("selected", ApplicationStatus.SELECTED)
                .setParameter("joined", ApplicationStatus.JOINED)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList().stream().map(row -> java.util.Map.of(
                        "job", row[0], "code", row[1], "applications", row[2], "shortlisted", row[3], "interviewed", row[4], "selected", row[5], "joined", row[6]
                )).toList();
    }

    @GetMapping("/api/reports/companies")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public List<?> companies() {
        return entityManager.createQuery("""
                select c.companyName, c.companyCode,
                (select count(j.id) from Job j where j.company = c and j.status = :published),
                (select count(p.id) from Placement p where p.company = c),
                (select count(e.id) from Employee e where e.company = c and e.status = :active)
                from Company c order by c.companyName
                """, Object[].class)
                .setParameter("published", JobStatus.PUBLISHED)
                .setParameter("active", EmployeeStatus.ACTIVE)
                .getResultList().stream().map(row -> java.util.Map.of(
                        "company", row[0], "code", row[1], "openJobs", row[2], "placements", row[3], "activeEmployees", row[4]
                )).toList();
    }

    @GetMapping("/api/reports/employees")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public List<?> employees() {
        return entityManager.createQuery("""
                select e.employeeCode, e.fullName, e.designation, coalesce(c.companyName, ''), coalesce(e.department, ''), e.status
                from Employee e left join e.company c order by e.fullName
                """, Object[].class).getResultList().stream().map(row -> java.util.Map.of(
                "employeeCode", row[0], "name", row[1], "designation", row[2] == null ? "" : row[2], "company", row[3], "department", row[4], "status", row[5]
        )).toList();
    }

    @GetMapping("/api/reports/payroll")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<?> payroll(@RequestParam(required = false) Integer year) {
        int target = year == null ? LocalDate.now().getYear() : year;
        return entityManager.createQuery("""
                select r.company.companyName, r.periodMonth, r.periodYear, r.status, r.totalGross, r.totalDeductions, r.totalNet
                from PayrollRun r where r.periodYear = :year order by r.periodMonth
                """, Object[].class).setParameter("year", target).getResultList().stream().map(row -> java.util.Map.of(
                "company", row[0], "month", row[1], "year", row[2], "status", row[3], "gross", row[4], "deductions", row[5], "net", row[6]
        )).toList();
    }

    @GetMapping("/api/reports/placements")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public List<?> placements() {
        return entityManager.createQuery("""
                select p.candidate.fullName, p.company.companyName, coalesce(p.designation, ''), p.joiningDate, p.placementStatus
                from Placement p order by p.createdAt desc
                """, Object[].class).getResultList().stream().map(row -> java.util.Map.of(
                "candidate", row[0], "company", row[1], "designation", row[2], "joiningDate", row[3] == null ? "" : row[3], "status", row[4]
        )).toList();
    }

    @GetMapping("/api/reports/export")
    @PreAuthorize("hasAuthority('REPORT_READ')")
    public void export(@RequestParam String type, @RequestParam(defaultValue = "csv") String format, HttpServletResponse response) throws IOException {
        List<?> rows = switch (type) {
            case "employees" -> employees();
            case "companies" -> companies();
            case "placements" -> placements();
            case "payroll" -> payroll(null);
            default -> recruitment(null, null);
        };
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> maps = (List<java.util.Map<String, Object>>) rows;
        if ("xlsx".equalsIgnoreCase(format)) {
            writeXlsx(type, maps, response);
        } else {
            writeCsv(type, maps, response);
        }
    }

    private void writeCsv(String type, List<java.util.Map<String, Object>> maps, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + type + ".csv\"");
        PrintWriter writer = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8);
        if (maps.isEmpty()) {
            writer.println("empty");
            writer.flush();
            return;
        }
        List<String> headers = new java.util.ArrayList<>(maps.get(0).keySet());
        writer.println(String.join(",", headers));
        for (var row : maps) {
            writer.println(headers.stream().map(key -> csv(row.get(key))).reduce((a, b) -> a + "," + b).orElse(""));
        }
        writer.flush();
    }

    private void writeXlsx(String type, List<java.util.Map<String, Object>> maps, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + type + ".xlsx\"");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(type);
            if (!maps.isEmpty()) {
                List<String> headers = new java.util.ArrayList<>(maps.get(0).keySet());
                Row header = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) header.createCell(i).setCellValue(headers.get(i));
                for (int r = 0; r < maps.size(); r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < headers.size(); c++) {
                        Object value = maps.get(r).get(headers.get(c));
                        row.createCell(c).setCellValue(value == null ? "" : String.valueOf(value));
                    }
                }
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
