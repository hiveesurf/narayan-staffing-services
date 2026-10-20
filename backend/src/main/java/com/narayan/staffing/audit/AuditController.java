package com.narayan.staffing.audit;

import com.narayan.staffing.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/api/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public PageResponse<AuditLog> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return auditService.search(q, action, from, to, page, size);
    }
}
