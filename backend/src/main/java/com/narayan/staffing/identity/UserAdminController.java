package com.narayan.staffing.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService service;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public com.narayan.staffing.common.PageResponse<AuthDtos.UserProfile> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public java.util.List<String> roles() {
        return service.roleNames();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public AuthDtos.UserProfile create(@Valid @RequestBody AuthDtos.UserRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public AuthDtos.UserProfile update(@PathVariable UUID id, @Valid @RequestBody AuthDtos.UserRequest request) {
        return service.update(id, request);
    }
}
