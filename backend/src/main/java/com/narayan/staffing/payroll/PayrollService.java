package com.narayan.staffing.payroll;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.CodeGenerator;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.company.CompanyService;
import com.narayan.staffing.domain.CalculationType;
import com.narayan.staffing.domain.ComponentType;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PayrollStatus;
import com.narayan.staffing.identity.UserRepository;
import com.narayan.staffing.notification.NotificationService;
import com.narayan.staffing.storage.StorageService;
import com.narayan.staffing.workforce.Employee;
import com.narayan.staffing.workforce.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final SalaryComponentDefinitionRepository definitions;
    private final SalaryStructureRepository structures;
    private final EmployeeSalaryComponentRepository structureComponents;
    private final SalaryRevisionRepository revisions;
    private final PayrollRunRepository runs;
    private final PayrollRecordRepository records;
    private final PayrollLineRepository lines;
    private final PayrollAdjustmentRepository adjustments;
    private final SalarySlipRepository slips;
    private final EmployeeRepository employees;
    private final CompanyService companies;
    private final UserRepository users;
    private final CodeGenerator codes;
    private final AuditService auditService;
    private final NotificationService notifications;
    private final SalarySlipPdfService pdfService;
    private final AttendanceService attendanceService;
    private final StorageService storageService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<SalaryComponentDefinition> components() {
        return definitions.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Transactional
    public SalaryComponentDefinition updateComponent(UUID id, CalculationType calculationType, BigDecimal defaultValue, boolean active) {
        SalaryComponentDefinition definition = definitions.findById(id).orElseThrow(() -> new NotFoundException("Component not found"));
        definition.setCalculationType(calculationType);
        definition.setDefaultValue(defaultValue);
        definition.setActive(active);
        return definition;
    }

    public record ComponentInput(UUID componentId, CalculationType calculationType, BigDecimal value) {
    }

    public record StructureRequest(@NotNull LocalDate effectiveDate, String reason, @NotNull List<ComponentInput> components) {
    }

    public record StructureResponse(UUID id, LocalDate effectiveDate, LocalDate endDate, boolean active, BigDecimal gross, BigDecimal deductions, BigDecimal net, String reason, List<LineView> lines) {
    }

    public record LineView(String code, String name, ComponentType type, CalculationType calculationType, BigDecimal value, BigDecimal amount) {
    }

    public record RunResponse(UUID id, UUID companyId, String companyName, int month, int year, PayrollStatus status,
                               BigDecimal totalGross, BigDecimal totalDeductions, BigDecimal totalNet, List<String> warnings, List<RecordResponse> records) {
    }

    public record RecordResponse(UUID id, UUID employeeId, String employeeCode, String employeeName, String designation, String department,
                                  int totalDays, int workingDays, BigDecimal payableDays, BigDecimal leaveDays,
                                  BigDecimal basic, BigDecimal gross, BigDecimal deductions, BigDecimal net, List<LineView> lines) {
    }

    public record RecordEdit(BigDecimal payableDays, Integer workingDays, BigDecimal leaveDays, List<AmountEdit> lines) {
    }

    public record AmountEdit(String code, BigDecimal amount) {
    }

    public record AdjustmentRequest(@NotNull BigDecimal amount, @NotNull String reason, UUID payrollRecordId) {
    }

    @Transactional
    public StructureResponse saveStructure(UUID employeeId, StructureRequest request) {
        Employee employee = employees.findById(employeeId).orElseThrow(() -> new NotFoundException("Employee not found"));
        List<SalaryStructure> existing = structures.findByEmployeeIdOrderByEffectiveDateDesc(employeeId);
        SalaryStructure previous = existing.stream().filter(SalaryStructure::isActive).findFirst().orElse(null);
        if (previous != null) {
            previous.setActive(false);
            previous.setEndDate(request.effectiveDate().minusDays(1));
        }
        SalaryStructure structure = new SalaryStructure();
        structure.setEmployee(employee);
        structure.setEffectiveDate(request.effectiveDate());
        structure.setReason(request.reason());
        structure.setActive(true);
        if (SecurityUtils.currentUserId() != null) structure.setChangedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        structures.save(structure);
        List<SalaryCalculator.ComponentInput> inputs = new ArrayList<>();
        for (ComponentInput input : request.components()) {
            SalaryComponentDefinition definition = definitions.findById(input.componentId()).orElseThrow(() -> new NotFoundException("Component not found"));
            EmployeeSalaryComponent row = new EmployeeSalaryComponent();
            row.setSalaryStructure(structure);
            row.setComponent(definition);
            row.setCalculationType(input.calculationType() == null ? definition.getCalculationType() : input.calculationType());
            row.setValue(input.value() == null ? BigDecimal.ZERO : input.value());
            structureComponents.save(row);
            inputs.add(new SalaryCalculator.ComponentInput(definition.getCode(), definition.getName(), definition.getComponentType(), row.getCalculationType(), row.getValue(), definition.getDisplayOrder()));
        }
        SalaryCalculator.Breakdown breakdown = SalaryCalculator.calculate(inputs, new BigDecimal("30"), new BigDecimal("30"));
        applyAmounts(structure, breakdown);
        structure.setGrossAmount(breakdown.gross());
        structure.setDeductionAmount(breakdown.deductions());
        structure.setNetAmount(breakdown.net());
        SalaryRevision revision = new SalaryRevision();
        revision.setEmployee(employee);
        revision.setPreviousStructure(previous);
        revision.setNewStructure(structure);
        revision.setEffectiveDate(request.effectiveDate());
        revision.setPreviousNet(previous == null ? null : previous.getNetAmount());
        revision.setNewNet(breakdown.net());
        revision.setReason(request.reason());
        revision.setChangedBy(structure.getChangedBy());
        revisions.save(revision);
        auditService.log("SALARY_CHANGED", "EMPLOYEE", employeeId.toString(), Map.of("net", breakdown.net().toString(), "effective", request.effectiveDate().toString()));
        return structureView(structure);
    }

    @Transactional(readOnly = true)
    public List<StructureResponse> structures(UUID employeeId) {
        return structures.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream().map(this::structureView).toList();
    }

    @Transactional
    public RunResponse createRun(UUID companyId, int month, int year) {
        if (month < 1 || month > 12) throw new BusinessException("Month must be between 1 and 12.");
        if (runs.findByCompanyIdAndPeriodMonthAndPeriodYear(companyId, month, year).isPresent()) {
            throw new ConflictException("A payroll run already exists for that company and month.");
        }
        PayrollRun run = new PayrollRun();
        run.setCompany(companies.require(companyId));
        run.setPeriodMonth(month);
        run.setPeriodYear(year);
        run.setStatus(PayrollStatus.DRAFT);
        runs.save(run);
        return generate(run.getId());
    }

    @Transactional
    public RunResponse generate(UUID runId) {
        PayrollRun run = requireRun(runId);
        ensureEditable(run);
        run.setStatus(PayrollStatus.PROCESSING);
        records.findByPayrollRunIdOrderByEmployeeNameAsc(runId).forEach(record -> {
            lines.deleteByPayrollRecordId(record.getId());
            slips.findByPayrollRecordId(record.getId()).ifPresent(slips::delete);
            records.delete(record);
        });
        entityManager.flush();
        LocalDate start = LocalDate.of(run.getPeriodYear(), run.getPeriodMonth(), 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        int calendarDays = end.getDayOfMonth();
        AttendanceService.AttendanceView attendance = attendanceService.view(run.getCompany().getId(), run.getPeriodMonth(), run.getPeriodYear());
        Map<UUID, AttendanceService.PersonDays> byEmployee = attendance.employees().stream()
                .collect(Collectors.toMap(AttendanceService.PersonDays::employeeId, Function.identity()));
        List<String> warnings = new ArrayList<>();
        List<Employee> people = employees.findByCompanyIdAndStatusIn(run.getCompany().getId(), List.of(EmployeeStatus.ACTIVE, EmployeeStatus.ON_LEAVE));
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deduction = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (Employee employee : people) {
            SalaryStructure structure = structures.findByEmployeeIdAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(employee.getId(), end).stream()
                    .filter(item -> item.getEndDate() == null || !item.getEndDate().isBefore(start))
                    .findFirst().orElse(null);
            AttendanceService.PersonDays attended = byEmployee.get(employee.getId());
            if (attendance.uploaded() && attended == null) {
                warnings.add(employee.getFullName() + " has no attendance for this month.");
                continue;
            }
            if (structure == null) {
                warnings.add(employee.getFullName() + " has no salary structure for this month.");
                continue;
            }
            List<EmployeeSalaryComponent> components = structureComponents.findBySalaryStructureId(structure.getId());
            List<SalaryCalculator.ComponentInput> inputs = components.stream().map(row -> new SalaryCalculator.ComponentInput(
                    row.getComponent().getCode(), row.getComponent().getName(), row.getComponent().getComponentType(),
                    row.getCalculationType(), row.getValue(), row.getComponent().getDisplayOrder()
            )).toList();
            BigDecimal payable = attendance.uploaded() ? attended.paidDays() : BigDecimal.valueOf(calendarDays);
            BigDecimal leave = attendance.uploaded() ? BigDecimal.valueOf(attended.leave()) : BigDecimal.ZERO;
            SalaryCalculator.Breakdown breakdown = SalaryCalculator.calculate(inputs, payable, BigDecimal.valueOf(calendarDays));
            PayrollRecord record = new PayrollRecord();
            record.setPayrollRun(run);
            record.setEmployee(employee);
            record.setSalaryStructure(structure);
            record.setEmployeeCode(employee.getEmployeeCode());
            record.setEmployeeName(employee.getFullName());
            record.setDesignation(employee.getDesignation());
            record.setDepartment(employee.getDepartment());
            record.setTotalDays(calendarDays);
            record.setWorkingDays(calendarDays);
            record.setPayableDays(payable);
            record.setLeaveDays(leave);
            record.setBasicAmount(breakdown.basic());
            record.setGrossAmount(breakdown.gross());
            record.setDeductionAmount(breakdown.deductions());
            record.setNetAmount(breakdown.net());
            records.save(record);
            saveLines(record, breakdown.lines());
            gross = gross.add(breakdown.gross());
            deduction = deduction.add(breakdown.deductions());
            net = net.add(breakdown.net());
        }
        if (attendance.uploaded()) {
            Set<UUID> activeIds = people.stream().map(Employee::getId).collect(Collectors.toSet());
            for (AttendanceService.PersonDays person : attendance.employees()) {
                if (!activeIds.contains(person.employeeId())) {
                    warnings.add(person.employeeName() + " is on the attendance sheet but is not an active employee.");
                }
            }
        }
        run.setTotalGross(gross);
        run.setTotalDeductions(deduction);
        run.setTotalNet(net);
        run.setStatus(PayrollStatus.DRAFT);
        if (SecurityUtils.currentUserId() != null) run.setGeneratedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        auditService.log("PAYROLL_GENERATED", "PAYROLL_RUN", run.getId().toString(), Map.of("net", net.toString()));
        notifications.email(run.getCompany().getEmail(), "PAYROLL_GENERATED", "Payroll draft ready",
                "Payroll for " + run.getCompany().getCompanyName() + " " + run.getPeriodMonth() + "/" + run.getPeriodYear() + " is ready for review.", "PAYROLL_RUN", run.getId());
        RunResponse response = toRun(run);
        return new RunResponse(response.id(), response.companyId(), response.companyName(), response.month(), response.year(), response.status(),
                response.totalGross(), response.totalDeductions(), response.totalNet(), warnings, response.records());
    }

    @Transactional
    public RecordResponse editRecord(UUID recordId, RecordEdit edit) {
        PayrollRecord record = records.findById(recordId).orElseThrow(() -> new NotFoundException("Payroll record not found"));
        ensureEditable(record.getPayrollRun());
        if (edit.workingDays() != null) record.setWorkingDays(edit.workingDays());
        if (edit.payableDays() != null) record.setPayableDays(edit.payableDays());
        if (edit.leaveDays() != null) record.setLeaveDays(edit.leaveDays());
        lines.deleteByPayrollRecordId(recordId);
        if (edit.lines() != null && !edit.lines().isEmpty()) {
            List<SalaryCalculator.Line> rebuilt = new ArrayList<>();
            List<PayrollLine> previous = new ArrayList<>();
            structureComponents.findBySalaryStructureId(record.getSalaryStructure().getId()).forEach(component -> previous.add(lineFrom(component)));
            for (PayrollLine existing : previous) {
                BigDecimal amount = edit.lines().stream().filter(line -> line.code().equals(existing.getComponentCode())).map(AmountEdit::amount).findFirst().orElse(existing.getAmount());
                rebuilt.add(new SalaryCalculator.Line(existing.getComponentCode(), existing.getComponentName(), existing.getLineType(), amount, existing.getDisplayOrder()));
            }
            BigDecimal gross = sum(rebuilt, ComponentType.EARNING);
            BigDecimal deduction = sum(rebuilt, ComponentType.DEDUCTION);
            record.setGrossAmount(gross);
            record.setDeductionAmount(deduction);
            record.setNetAmount(gross.subtract(deduction));
            record.setBasicAmount(rebuilt.stream().filter(line -> SalaryCalculator.BASIC.equals(line.code())).map(SalaryCalculator.Line::amount).findFirst().orElse(BigDecimal.ZERO));
            saveLines(record, rebuilt);
        } else if (record.getSalaryStructure() != null) {
            List<SalaryCalculator.ComponentInput> inputs = structureComponents.findBySalaryStructureId(record.getSalaryStructure().getId()).stream()
                    .map(row -> new SalaryCalculator.ComponentInput(row.getComponent().getCode(), row.getComponent().getName(), row.getComponent().getComponentType(), row.getCalculationType(), row.getValue(), row.getComponent().getDisplayOrder()))
                    .toList();
            SalaryCalculator.Breakdown breakdown = SalaryCalculator.calculate(inputs, record.getPayableDays(), BigDecimal.valueOf(record.getWorkingDays()));
            record.setBasicAmount(breakdown.basic());
            record.setGrossAmount(breakdown.gross());
            record.setDeductionAmount(breakdown.deductions());
            record.setNetAmount(breakdown.net());
            saveLines(record, breakdown.lines());
        }
        retotal(record.getPayrollRun());
        return toRecord(record);
    }

    @Transactional
    public RunResponse approve(UUID runId) {
        PayrollRun run = requireRun(runId);
        if (run.getStatus() == PayrollStatus.LOCKED) throw new BusinessException("Locked payroll cannot be approved again.");
        run.setStatus(PayrollStatus.APPROVED);
        run.setApprovedAt(Instant.now());
        if (SecurityUtils.currentUserId() != null) run.setApprovedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        auditService.log("PAYROLL_APPROVED", "PAYROLL_RUN", runId.toString(), Map.of());
        return toRun(run);
    }

    @Transactional
    public RunResponse lock(UUID runId) {
        PayrollRun run = requireRun(runId);
        if (run.getStatus() != PayrollStatus.APPROVED) throw new BusinessException("Approve the payroll before locking it.");
        run.setStatus(PayrollStatus.LOCKED);
        run.setLockedAt(Instant.now());
        if (SecurityUtils.currentUserId() != null) run.setLockedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        auditService.log("PAYROLL_LOCKED", "PAYROLL_RUN", runId.toString(), Map.of());
        records.findByPayrollRunIdOrderByEmployeeNameAsc(runId).forEach(record -> notifications.email(record.getEmployee().getCandidate().getEmail(), "SALARY_SLIP_AVAILABLE",
                "Salary slip available", "Your salary slip for " + run.getPeriodMonth() + "/" + run.getPeriodYear() + " is available with the payroll desk.", "PAYROLL_RECORD", record.getId()));
        return toRun(run);
    }

    @Transactional
    public PayrollAdjustment adjust(UUID runId, AdjustmentRequest request) {
        PayrollRun run = requireRun(runId);
        if (run.getStatus() != PayrollStatus.LOCKED) throw new BusinessException("Adjustments are recorded after payroll is locked.");
        PayrollAdjustment adjustment = new PayrollAdjustment();
        adjustment.setPayrollRun(run);
        if (request.payrollRecordId() != null) {
            adjustment.setPayrollRecord(records.findById(request.payrollRecordId()).orElseThrow(() -> new NotFoundException("Payroll record not found")));
        }
        adjustment.setAmount(request.amount());
        adjustment.setReason(request.reason());
        if (SecurityUtils.currentUserId() != null) adjustment.setCreatedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
        adjustments.save(adjustment);
        auditService.log("PAYROLL_ADJUSTMENT", "PAYROLL_RUN", runId.toString(), Map.of("amount", request.amount().toString()));
        return adjustment;
    }

    @Transactional(readOnly = true)
    public PageResponse<RunResponse> listRuns(UUID companyId, Integer year, int page, int size) {
        var pageable = PageRequests.of(page, size, "periodYear", "desc", Set.of("periodYear", "createdAt"), "createdAt");
        return PageResponse.from(runs.findAll((root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (companyId != null) predicates.add(cb.equal(root.get("company").get("id"), companyId));
            if (year != null) predicates.add(cb.equal(root.get("periodYear"), year));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable).map(run -> {
            RunResponse full = toRun(run);
            return new RunResponse(full.id(), full.companyId(), full.companyName(), full.month(), full.year(), full.status(), full.totalGross(), full.totalDeductions(), full.totalNet(), List.of(), List.of());
        }));
    }

    @Transactional(readOnly = true)
    public RunResponse getRun(UUID id) {
        return toRun(requireRun(id));
    }

    @Transactional
    public SalarySlip slipForRecord(UUID recordId) {
        PayrollRecord record = records.findById(recordId).orElseThrow(() -> new NotFoundException("Payroll record not found"));
        return slips.findByPayrollRecordId(recordId).orElseGet(() -> createSlip(record));
    }

    @Transactional
    public byte[] slipPdf(UUID slipId) {
        SalarySlip slip = slips.findById(slipId).orElseThrow(() -> new NotFoundException("Salary slip not found"));
        List<PayrollLine> payLines = lines.findByPayrollRecordIdOrderByDisplayOrderAsc(slip.getPayrollRecord().getId());
        byte[] pdf = pdfService.render(slip.getPayrollRecord(), payLines, slip.getSlipNumber());
        if (slip.getStoragePath() == null) {
            StorageService.StoredFile stored = storageService.store("salary-slips", slip.getSlipNumber() + ".pdf", new ByteArrayInputStream(pdf), pdf.length);
            slip.setStoragePath(stored.storagePath());
            slip.setGeneratedAt(Instant.now());
            if (SecurityUtils.currentUserId() != null) slip.setGeneratedBy(users.findById(SecurityUtils.currentUserId()).orElse(null));
            auditService.log("SALARY_SLIP_GENERATED", "SALARY_SLIP", slip.getId().toString(), Map.of("number", slip.getSlipNumber()));
        }
        return pdf;
    }

    public record SlipSummary(UUID recordId, String slipNumber, int month, int year, String companyName, BigDecimal gross, BigDecimal net) {
    }

    @Transactional(readOnly = true)
    public List<SlipSummary> employeeSlips(UUID employeeId) {
        return records.findByEmployeeId(employeeId).stream()
                .sorted(Comparator.comparingInt((PayrollRecord record) -> record.getPayrollRun().getPeriodYear())
                        .thenComparingInt(record -> record.getPayrollRun().getPeriodMonth())
                        .reversed())
                .map(record -> {
                    SalarySlip slip = slips.findByPayrollRecordId(record.getId()).orElse(null);
                    return new SlipSummary(
                            record.getId(),
                            slip == null ? null : slip.getSlipNumber(),
                            record.getPayrollRun().getPeriodMonth(),
                            record.getPayrollRun().getPeriodYear(),
                            record.getPayrollRun().getCompany().getCompanyName(),
                            record.getGrossAmount(),
                            record.getNetAmount());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SalarySlip> slipsForEmployee(UUID employeeId) {
        return records.findByEmployeeId(employeeId).stream().map(record -> slips.findByPayrollRecordId(record.getId()).orElse(null)).filter(java.util.Objects::nonNull).toList();
    }

    private SalarySlip createSlip(PayrollRecord record) {
        SalarySlip slip = new SalarySlip();
        slip.setPayrollRecord(record);
        slip.setSlipNumber(codes.next("slip_no_seq", "NSS-SLIP-" + Year.now().getValue() + "-", 5));
        return slips.save(slip);
    }

    private void saveLines(PayrollRecord record, List<SalaryCalculator.Line> calculated) {
        calculated.stream().sorted(Comparator.comparingInt(SalaryCalculator.Line::displayOrder)).forEach(line -> {
            PayrollLine entity = new PayrollLine();
            entity.setPayrollRecord(record);
            entity.setComponentCode(line.code());
            entity.setComponentName(line.name());
            entity.setLineType(line.type());
            entity.setAmount(line.amount());
            entity.setDisplayOrder(line.displayOrder());
            lines.save(entity);
        });
    }

    private void applyAmounts(SalaryStructure structure, SalaryCalculator.Breakdown breakdown) {
        List<EmployeeSalaryComponent> rows = structureComponents.findBySalaryStructureId(structure.getId());
        for (EmployeeSalaryComponent row : rows) {
            breakdown.lines().stream().filter(line -> line.code().equals(row.getComponent().getCode())).findFirst()
                    .ifPresent(line -> row.setCalculatedAmount(line.amount()));
        }
    }

    private PayrollLine lineFrom(EmployeeSalaryComponent component) {
        PayrollLine line = new PayrollLine();
        line.setComponentCode(component.getComponent().getCode());
        line.setComponentName(component.getComponent().getName());
        line.setLineType(component.getComponent().getComponentType());
        line.setAmount(component.getCalculatedAmount());
        line.setDisplayOrder(component.getComponent().getDisplayOrder());
        return line;
    }

    private BigDecimal sum(List<SalaryCalculator.Line> lines, ComponentType type) {
        return lines.stream().filter(line -> line.type() == type).map(SalaryCalculator.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void retotal(PayrollRun run) {
        List<PayrollRecord> all = records.findByPayrollRunIdOrderByEmployeeNameAsc(run.getId());
        run.setTotalGross(all.stream().map(PayrollRecord::getGrossAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalDeductions(all.stream().map(PayrollRecord::getDeductionAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        run.setTotalNet(all.stream().map(PayrollRecord::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private void ensureEditable(PayrollRun run) {
        if (run.getStatus() == PayrollStatus.APPROVED || run.getStatus() == PayrollStatus.LOCKED) {
            throw new BusinessException("This payroll is " + run.getStatus().name().toLowerCase() + " and cannot be regenerated. Record an adjustment after it is locked.");
        }
    }

    private PayrollRun requireRun(UUID id) {
        return runs.findById(id).orElseThrow(() -> new NotFoundException("Payroll run not found"));
    }

    private StructureResponse structureView(SalaryStructure structure) {
        List<LineView> views = structureComponents.findBySalaryStructureId(structure.getId()).stream()
                .sorted(Comparator.comparingInt(row -> row.getComponent().getDisplayOrder()))
                .map(row -> new LineView(row.getComponent().getCode(), row.getComponent().getName(), row.getComponent().getComponentType(), row.getCalculationType(), row.getValue(), row.getCalculatedAmount()))
                .toList();
        return new StructureResponse(structure.getId(), structure.getEffectiveDate(), structure.getEndDate(), structure.isActive(), structure.getGrossAmount(), structure.getDeductionAmount(), structure.getNetAmount(), structure.getReason(), views);
    }

    private RunResponse toRun(PayrollRun run) {
        List<RecordResponse> rows = records.findByPayrollRunIdOrderByEmployeeNameAsc(run.getId()).stream().map(this::toRecord).toList();
        return new RunResponse(run.getId(), run.getCompany().getId(), run.getCompany().getCompanyName(), run.getPeriodMonth(), run.getPeriodYear(), run.getStatus(),
                run.getTotalGross(), run.getTotalDeductions(), run.getTotalNet(), List.of(), rows);
    }

    private RecordResponse toRecord(PayrollRecord record) {
        List<LineView> views = lines.findByPayrollRecordIdOrderByDisplayOrderAsc(record.getId()).stream()
                .map(line -> new LineView(line.getComponentCode(), line.getComponentName(), line.getLineType(), null, null, line.getAmount()))
                .toList();
        return new RecordResponse(record.getId(), record.getEmployee().getId(), record.getEmployeeCode(), record.getEmployeeName(), record.getDesignation(), record.getDepartment(),
                record.getTotalDays(), record.getWorkingDays(), record.getPayableDays(), record.getLeaveDays(), record.getBasicAmount(), record.getGrossAmount(), record.getDeductionAmount(), record.getNetAmount(), views);
    }
}
