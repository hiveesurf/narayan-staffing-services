package com.narayan.staffing.payroll;

import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.company.Company;
import com.narayan.staffing.company.CompanyRepository;
import com.narayan.staffing.workforce.Employee;
import com.narayan.staffing.workforce.EmployeeRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSheetRepository sheets;
    private final AttendanceDayRepository days;
    private final CompanyRepository companies;
    private final EmployeeRepository employees;
    private final EntityManager entityManager;

    public record DayView(LocalDate date, String status) {
    }

    public record PersonDays(UUID employeeId, String employeeCode, String employeeName, int present, int absent, int leave, int weeklyOff,
                              BigDecimal paidDays, List<DayView> days) {
    }

    public record AttendanceView(boolean uploaded, String fileName, int month, int year, List<PersonDays> employees) {
    }

    public record DayWrite(UUID employeeId, LocalDate date, AttendanceStatus status) {
    }

    public byte[] template() {
        return AttendanceWorkbook.template();
    }

    @Transactional(readOnly = true)
    public AttendanceView view(UUID companyId, int month, int year) {
        requirePeriod(month, year);
        return sheets.findByCompanyIdAndPeriodMonthAndPeriodYear(companyId, month, year)
                .map(this::toView)
                .orElseGet(() -> new AttendanceView(false, null, month, year, List.of()));
    }

    @Transactional
    public AttendanceView importFile(UUID companyId, int month, int year, MultipartFile file) {
        requirePeriod(month, year);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Choose an Excel file.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            throw new BusinessException("Upload an .xlsx file.");
        }
        Company company = companies.findById(companyId).orElseThrow(() -> new NotFoundException("Company not found"));
        List<AttendanceWorkbook.RowDay> rows;
        try {
            rows = AttendanceWorkbook.read(file.getInputStream(), YearMonth.of(year, month));
        } catch (IOException ex) {
            throw new BusinessException("The file could not be read.");
        }
        Map<String, Employee> byCode = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        List<DayWrite> writes = new ArrayList<>();
        for (AttendanceWorkbook.RowDay row : rows) {
            Employee employee = byCode.computeIfAbsent(row.employeeCode(), code -> employees.findByEmployeeCode(code).orElse(null));
            if (employee == null || employee.getCompany() == null || !company.getId().equals(employee.getCompany().getId())) {
                errors.add("Row " + row.rowNumber() + " employee " + row.employeeCode() + " is not at " + company.getCompanyName() + ".");
                continue;
            }
            boolean duplicate = writes.stream().anyMatch(item -> item.employeeId().equals(employee.getId()) && item.date().equals(row.date()));
            if (duplicate) {
                errors.add("Row " + row.rowNumber() + " repeats " + row.employeeCode() + " on " + row.date() + ".");
                continue;
            }
            writes.add(new DayWrite(employee.getId(), row.date(), row.status()));
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(" ", errors.stream().limit(8).toList()));
        }
        replace(company, month, year, file.getOriginalFilename(), writes);
        return view(companyId, month, year);
    }

    @Transactional
    public void replace(Company company, int month, int year, String fileName, List<DayWrite> writes) {
        sheets.findByCompanyIdAndPeriodMonthAndPeriodYear(company.getId(), month, year).ifPresent(existing -> {
            sheets.delete(existing);
            entityManager.flush();
        });
        AttendanceSheet sheet = new AttendanceSheet();
        sheet.setCompany(company);
        sheet.setPeriodMonth(month);
        sheet.setPeriodYear(year);
        sheet.setFileName(fileName);
        sheets.save(sheet);
        for (DayWrite write : writes) {
            AttendanceDay day = new AttendanceDay();
            day.setSheet(sheet);
            day.setEmployee(employees.findById(write.employeeId()).orElseThrow(() -> new NotFoundException("Employee not found")));
            day.setWorkDate(write.date());
            day.setStatus(write.status());
            days.save(day);
        }
    }

    private AttendanceView toView(AttendanceSheet sheet) {
        Map<UUID, PersonBuilder> grouped = new LinkedHashMap<>();
        List<AttendanceDay> ordered = days.findBySheetId(sheet.getId()).stream()
                .sorted(Comparator.comparing((AttendanceDay day) -> day.getEmployee().getFullName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceDay::getWorkDate))
                .toList();
        for (AttendanceDay day : ordered) {
            PersonBuilder person = grouped.computeIfAbsent(day.getEmployee().getId(), id -> new PersonBuilder(day.getEmployee()));
            person.add(day);
        }
        List<PersonDays> people = grouped.values().stream().map(PersonBuilder::build).toList();
        return new AttendanceView(true, sheet.getFileName(), sheet.getPeriodMonth(), sheet.getPeriodYear(), people);
    }

    private void requirePeriod(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new BusinessException("Year is not valid.");
        }
    }

    private static final class PersonBuilder {
        private final Employee employee;
        private int present;
        private int absent;
        private int leave;
        private int weeklyOff;
        private final List<DayView> days = new ArrayList<>();

        private PersonBuilder(Employee employee) {
            this.employee = employee;
        }

        private void add(AttendanceDay day) {
            switch (day.getStatus()) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case LEAVE -> leave++;
                case WEEKLY_OFF -> weeklyOff++;
            }
            days.add(new DayView(day.getWorkDate(), day.getStatus().name()));
        }

        private PersonDays build() {
            return new PersonDays(employee.getId(), employee.getEmployeeCode(), employee.getFullName(), present, absent, leave, weeklyOff,
                    BigDecimal.valueOf(present + weeklyOff), List.copyOf(days));
        }
    }
}
