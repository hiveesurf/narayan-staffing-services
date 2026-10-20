package com.narayan.staffing.identity;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.PageRequests;
import com.narayan.staffing.common.PageResponse;
import com.narayan.staffing.common.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AuthDtos.UserProfile> list(String q, int page, int size) {
        Pageable pageable = PageRequests.of(page, size, "fullName", "asc", Set.of("fullName", "email", "createdAt"), "fullName");
        Specification<User> spec = (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(cb.like(cb.lower(root.get("fullName")), like), cb.like(cb.lower(root.get("email")), like));
        };
        return PageResponse.from(users.findAll(spec, pageable).map(authService::profile));
    }

    @Transactional(readOnly = true)
    public List<Role> roles() {
        return roles.findAll();
    }

    @Transactional
    public AuthDtos.UserProfile create(AuthDtos.UserRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("A password is required for a new user.");
        }
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with that email already exists.");
        }
        authService.validatePassword(request.password());
        User user = new User();
        apply(user, request, true);
        users.save(user);
        auditService.log("USER_CREATED", "USER", user.getId().toString(), Map.of("email", user.getEmail()));
        return authService.profile(user);
    }

    @Transactional
    public AuthDtos.UserProfile update(java.util.UUID id, AuthDtos.UserRequest request) {
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equalsIgnoreCase(request.email()) && users.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("A user with that email already exists.");
        }
        apply(user, request, false);
        if (request.password() != null && !request.password().isBlank()) {
            authService.validatePassword(request.password());
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        auditService.log("USER_PERMISSION_CHANGED", "USER", user.getId().toString(), Map.of("roles", request.roles()));
        return authService.profile(user);
    }

    private void apply(User user, AuthDtos.UserRequest request, boolean creating) {
        boolean actorIsSuper = SecurityUtils.hasAuthority("ROLE_SUPER_ADMIN");
        boolean assignsSuper = request.roles().stream().anyMatch("SUPER_ADMIN"::equals);
        boolean currentlySuper = user.getRoles().stream().anyMatch(role -> "SUPER_ADMIN".equals(role.getName()));
        if ((assignsSuper || currentlySuper) && !actorIsSuper) {
            throw new AccessDeniedException("Only a super admin can change a super admin account.");
        }
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setActive(request.active());
        if (creating) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        Set<Role> next = new HashSet<>();
        for (String name : request.roles()) {
            next.add(roles.findByName(name).orElseThrow(() -> new BusinessException("Unknown role: " + name)));
        }
        if (next.isEmpty()) {
            throw new BusinessException("Select at least one role.");
        }
        user.getRoles().clear();
        user.getRoles().addAll(next);
    }

    @Transactional(readOnly = true)
    public List<String> roleNames() {
        List<String> names = new ArrayList<>();
        roles.findAll().forEach(role -> names.add(role.getName()));
        return names;
    }
}
