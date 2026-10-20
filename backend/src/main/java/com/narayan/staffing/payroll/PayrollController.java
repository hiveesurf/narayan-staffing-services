package com.narayan.staffing.payroll;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final AttendanceService attendanceService;

    @GetMapping("/api/salary-components")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public List<SalaryComponentDefinition> components() {
        return payrollService.components();
    }

    @PutMapping("/api/salary-components/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public SalaryComponentDefinition updateComponent(@PathVariable UUID id, @RequestBody ComponentUpdate update) {
        return payrollService.updateComponent(id, update.calculationType(), update.defaultValue(), update.active());
    }

    @GetMapping("/api/employees/{employeeId}/salary-slips")
    @PreAuthorize("hasAuthority('PAYROLL_READ') or hasAuthority('EMPLOYEE_READ')")
    public List<PayrollService.SlipSummary> slips(@PathVariable UUID employeeId) {
        return payrollService.employeeSlips(employeeId);
    }

    @GetMapping("/api/employees/{employeeId}/salary-structures")
    @PreAuthorize("hasAuthority('PAYROLL_READ') or hasAuthority('EMPLOYEE_READ')")
    public List<PayrollService.StructureResponse> structures(@PathVariable UUID employeeId) {
        return payrollService.structures(employeeId);
    }

    @PostMapping("/api/employees/{employeeId}/salary-structures")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public PayrollService.StructureResponse saveStructure(@PathVariable UUID employeeId, @Valid @RequestBody PayrollService.StructureRequest request) {
        return payrollService.saveStructure(employeeId, request);
    }

    @GetMapping("/api/payroll/attendance/template")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public ResponseEntity<byte[]> attendanceTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"attendance-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(attendanceService.template());
    }

    @GetMapping("/api/payroll/attendance")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public AttendanceService.AttendanceView attendance(@RequestParam UUID companyId, @RequestParam int month, @RequestParam int year) {
        return attendanceService.view(companyId, month, year);
    }

    @PostMapping(value = "/api/payroll/attendance", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public AttendanceService.AttendanceView uploadAttendance(@RequestParam UUID companyId, @RequestParam int month, @RequestParam int year,
                                                             @RequestParam("file") MultipartFile file) {
        return attendanceService.importFile(companyId, month, year, file);
    }

    @GetMapping("/api/payroll/runs")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public com.narayan.staffing.common.PageResponse<PayrollService.RunResponse> runs(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return payrollService.listRuns(companyId, year, page, size);
    }

    @PostMapping("/api/payroll/runs")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public PayrollService.RunResponse create(@RequestParam UUID companyId, @RequestParam int month, @RequestParam int year) {
        return payrollService.createRun(companyId, month, year);
    }

    @GetMapping("/api/payroll/runs/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public PayrollService.RunResponse get(@PathVariable UUID id) {
        return payrollService.getRun(id);
    }

    @PostMapping("/api/payroll/runs/{id}/generate")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public PayrollService.RunResponse generate(@PathVariable UUID id) {
        return payrollService.generate(id);
    }

    @PutMapping("/api/payroll/records/{id}")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public PayrollService.RecordResponse edit(@PathVariable UUID id, @RequestBody PayrollService.RecordEdit edit) {
        return payrollService.editRecord(id, edit);
    }

    @PostMapping("/api/payroll/runs/{id}/approve")
    @PreAuthorize("hasAuthority('PAYROLL_APPROVE')")
    public PayrollService.RunResponse approve(@PathVariable UUID id) {
        return payrollService.approve(id);
    }

    @PostMapping("/api/payroll/runs/{id}/lock")
    @PreAuthorize("hasAuthority('PAYROLL_LOCK')")
    public PayrollService.RunResponse lock(@PathVariable UUID id) {
        return payrollService.lock(id);
    }

    @PostMapping("/api/payroll/runs/{id}/adjustments")
    @PreAuthorize("hasAuthority('PAYROLL_WRITE')")
    public PayrollAdjustment adjust(@PathVariable UUID id, @Valid @RequestBody PayrollService.AdjustmentRequest request) {
        return payrollService.adjust(id, request);
    }

    @PostMapping("/api/payroll/records/{id}/slip")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public SlipCreated createSlip(@PathVariable UUID id) {
        SalarySlip slip = payrollService.slipForRecord(id);
        return new SlipCreated(slip.getId(), slip.getSlipNumber());
    }

    @GetMapping("/api/salary-slips/{id}/pdf")
    @PreAuthorize("hasAuthority('PAYROLL_READ')")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] pdf = payrollService.slipPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"salary-slip.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    public record ComponentUpdate(com.narayan.staffing.domain.CalculationType calculationType, BigDecimal defaultValue, boolean active) {
    }

    public record SlipCreated(UUID id, String slipNumber) {
    }
}
