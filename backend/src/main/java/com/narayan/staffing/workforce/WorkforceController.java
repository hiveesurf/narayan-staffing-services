package com.narayan.staffing.workforce;

import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.document.DocumentService;
import com.narayan.staffing.domain.EmployeeStatus;
import com.narayan.staffing.domain.PlacementStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class WorkforceController {

    private final WorkforceService workforceService;
    private final DocumentService documentService;

    @PostMapping("/api/employees")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse create(@Valid @RequestBody WorkforceDtos.EmployeeCreate request) {
        return workforceService.createEmployee(request);
    }

    @PostMapping("/api/employees/{id}/photo")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse photo(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        var doc = documentService.store("EMPLOYEE", id, "PHOTO", file, true, true);
        workforceService.updatePhoto(id, "/api/public/media/" + doc.getId());
        return workforceService.employee(id).employee();
    }

    @PostMapping("/api/employees/{id}/resume")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeProfile resume(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        workforceService.employee(id);
        documentService.store("EMPLOYEE", id, "RESUME", file, false, false);
        return workforceService.employee(id);
    }

    @PostMapping("/api/employees/from-candidate/{candidateId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PLACEMENT_WRITE') or hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse fromCandidate(@PathVariable UUID candidateId) {
        return workforceService.employee(workforceService.ensureEmployee(candidateId).getId()).employee();
    }

    @GetMapping("/api/employees")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public PageResponse<WorkforceDtos.EmployeeResponse> employees(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return workforceService.employees(q, status, companyId, page, size, sort, direction);
    }

    @GetMapping("/api/employees/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_READ')")
    public WorkforceDtos.EmployeeProfile employee(@PathVariable UUID id) {
        return workforceService.employee(id);
    }

    @PutMapping("/api/employees/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse update(@PathVariable UUID id, @Valid @RequestBody WorkforceDtos.EmployeeUpdate request) {
        return workforceService.updateEmployee(id, request);
    }

    @PutMapping("/api/employees/{id}/bank")
    @PreAuthorize("hasAuthority('SENSITIVE_READ') and hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse bank(@PathVariable UUID id, @Valid @RequestBody WorkforceDtos.BankUpdate request) {
        return workforceService.updateBank(id, request);
    }

    @PatchMapping("/api/employees/{id}/status")
    @PreAuthorize("hasAuthority('EMPLOYEE_WRITE')")
    public WorkforceDtos.EmployeeResponse status(@PathVariable UUID id, @Valid @RequestBody WorkforceDtos.EmployeeStatusUpdate request) {
        return workforceService.changeEmployeeStatus(id, request.status(), request.reason(), request.note());
    }

    @GetMapping("/api/placements")
    @PreAuthorize("hasAuthority('PLACEMENT_READ')")
    public PageResponse<WorkforceDtos.PlacementResponse> placements(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PlacementStatus status,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return workforceService.placements(q, status, companyId, page, size);
    }

    @PostMapping("/api/placements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PLACEMENT_WRITE')")
    public WorkforceDtos.PlacementResponse create(@Valid @RequestBody WorkforceDtos.PlacementRequest request) {
        return workforceService.createPlacement(request);
    }

    @PatchMapping("/api/placements/{id}/status")
    @PreAuthorize("hasAuthority('PLACEMENT_WRITE')")
    public WorkforceDtos.PlacementResponse placementStatus(@PathVariable UUID id, @Valid @RequestBody WorkforceDtos.StatusNote request) {
        return workforceService.updatePlacementStatus(id, request.status(), request.reason(), request.note());
    }
}
