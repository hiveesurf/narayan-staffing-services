package com.narayan.staffing.bootstrap;

import com.narayan.staffing.payroll.AttendanceService;
import com.narayan.staffing.payroll.AttendanceStatus;
import com.narayan.staffing.payroll.PayrollRecordRepository;
import com.narayan.staffing.payroll.PayrollRunRepository;
import com.narayan.staffing.payroll.PayrollService;
import com.narayan.staffing.payroll.SalaryComponentDefinitionRepository;
import com.narayan.staffing.payroll.SalaryStructureRepository;
import com.narayan.staffing.workforce.Employee;
import com.narayan.staffing.workforce.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class PayrollDemoSeeder implements CommandLineRunner {

    private static final int MONTH = 9;
    private static final int YEAR = 2026;

    private final EmployeeRepository employees;
    private final SalaryStructureRepository structures;
    private final SalaryComponentDefinitionRepository definitions;
    private final PayrollRunRepository runs;
    private final PayrollRecordRepository records;
    private final PayrollService payroll;
    private final AttendanceService attendance;

    @Override
    @Transactional
    public void run(String... args) {
        Employee employee = employees.findByEmployeeCode("NSS-EMP-00001")
                .or(() -> employees.findFirstByFullNameIgnoreCase("Ramesh Kale"))
                .orElse(null);
        if (employee == null || employee.getCompany() == null) {
            return;
        }
        boolean alreadyPaid = records.findByEmployeeId(employee.getId()).stream().anyMatch(record ->
                record.getPayrollRun().getPeriodMonth() == MONTH && record.getPayrollRun().getPeriodYear() == YEAR);
        if (alreadyPaid) {
            return;
        }
        if (structures.findByEmployeeIdOrderByEffectiveDateDesc(employee.getId()).isEmpty()) {
            List<PayrollService.ComponentInput> inputs = definitions.findByActiveTrueOrderByDisplayOrderAsc().stream()
                    .map(definition -> new PayrollService.ComponentInput(
                            definition.getId(),
                            definition.getCalculationType(),
                            "BASIC".equals(definition.getCode()) ? new BigDecimal("25000") : definition.getDefaultValue()))
                    .toList();
            LocalDate effective = employee.getJoiningDate() == null ? LocalDate.of(2026, 4, 1) : employee.getJoiningDate();
            payroll.saveStructure(employee.getId(), new PayrollService.StructureRequest(effective, "Demo structure for September payroll", inputs));
        }
        List<AttendanceService.DayWrite> days = new ArrayList<>();
        for (int day = 1; day <= 30; day++) {
            AttendanceStatus status = day <= 26 ? AttendanceStatus.PRESENT : day <= 28 ? AttendanceStatus.LEAVE : AttendanceStatus.ABSENT;
            days.add(new AttendanceService.DayWrite(employee.getId(), LocalDate.of(YEAR, MONTH, day), status));
        }
        attendance.replace(employee.getCompany(), MONTH, YEAR, "deccan-september-2026.xlsx", days);
        runs.findByCompanyIdAndPeriodMonthAndPeriodYear(employee.getCompany().getId(), MONTH, YEAR)
                .ifPresentOrElse(run -> payroll.generate(run.getId()), () -> payroll.createRun(employee.getCompany().getId(), MONTH, YEAR));
        log.info("September {} payroll prepared for {}", YEAR, employee.getFullName());
    }
}
