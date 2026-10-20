package com.narayan.staffing.audit;

import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.security.StaffUserPrincipal;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Transactional
    public void log(String action, String entityType, String entityId, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog();
        StaffUserPrincipal principal = SecurityUtils.principal();
        if (principal != null) {
            entry.setUserId(principal.getId());
            entry.setUserEmail(principal.getEmail());
        }
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setIpAddress(clientIp());
        entry.setMetadata(metadata);
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> search(String q, String action, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, "createdAt", "desc", Set.of("createdAt", "action"), "createdAt");
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("userEmail")), like),
                        cb.like(cb.lower(root.get("entityType")), like),
                        cb.like(cb.lower(root.get("action")), like)
                ));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(repository.findAll(spec, pageable));
    }

    private String clientIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
