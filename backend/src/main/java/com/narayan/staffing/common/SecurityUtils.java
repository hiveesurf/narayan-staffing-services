package com.narayan.staffing.common;

import com.narayan.staffing.security.StaffUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static StaffUserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof StaffUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static UUID currentUserId() {
        StaffUserPrincipal principal = principal();
        return principal == null ? null : principal.getId();
    }

    public static boolean hasAuthority(String authority) {
        StaffUserPrincipal principal = principal();
        if (principal == null) {
            return false;
        }
        return principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }
}
