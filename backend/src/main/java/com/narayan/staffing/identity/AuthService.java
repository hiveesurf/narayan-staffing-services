package com.narayan.staffing.identity;

import com.narayan.staffing.audit.AuditService;
import com.narayan.staffing.common.BusinessException;
import com.narayan.staffing.common.ConflictException;
import com.narayan.staffing.common.NotFoundException;
import com.narayan.staffing.common.SecurityUtils;
import com.narayan.staffing.common.TokenHashes;
import com.narayan.staffing.config.AppProperties;
import com.narayan.staffing.notification.NotificationService;
import com.narayan.staffing.security.DatabaseUserDetailsService;
import com.narayan.staffing.security.JwtService;
import com.narayan.staffing.security.StaffUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DatabaseUserDetailsService userDetailsService;
    private final AppProperties properties;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request, String ip, String userAgent) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("bad"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("bad");
        }
        user.setLastLoginAt(Instant.now());
        StaffUserPrincipal principal = userDetailsService.toPrincipal(user);
        String refresh = issueRefresh(user, ip, userAgent);
        auditService.log("LOGIN", "USER", user.getId().toString(), Map.of("email", user.getEmail()));
        return tokens(principal, refresh);
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(String rawToken, String ip, String userAgent) {
        RefreshToken stored = refreshTokens.findByTokenHash(TokenHashes.sha256(rawToken))
                .orElseThrow(() -> new BadCredentialsException("bad"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("bad");
        }
        stored.setRevoked(true);
        User user = stored.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("bad");
        }
        String refresh = issueRefresh(user, ip, userAgent);
        return tokens(userDetailsService.toPrincipal(user), refresh);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(TokenHashes.sha256(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            auditService.log("LOGOUT", "USER", token.getUser().getId().toString(), Map.of());
        });
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserProfile me() {
        UUID id = SecurityUtils.currentUserId();
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return profile(user);
    }

    @Transactional
    public AuthDtos.MessageResponse forgot(String email) {
        users.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String raw = TokenHashes.randomToken();
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(TokenHashes.sha256(raw));
            token.setExpiresAt(Instant.now().plusSeconds(30 * 60));
            resetTokens.save(token);
            String link = properties.getPublicBaseUrl() + "/admin/reset-password?token=" + raw;
            notificationService.email(user.getEmail(), "PASSWORD_RESET", "Reset your Narayan Staffing password",
                    "Use this link within 30 minutes to choose a new password:\n" + link, "USER", user.getId());
        });
        return new AuthDtos.MessageResponse("If that email is registered, a reset link has been sent.");
    }

    @Transactional
    public AuthDtos.MessageResponse reset(String rawToken, String password) {
        PasswordResetToken token = resetTokens.findByTokenHash(TokenHashes.sha256(rawToken))
                .orElseThrow(() -> new BusinessException("This reset link is invalid or has expired."));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("This reset link is invalid or has expired.");
        }
        validatePassword(password);
        token.setUsed(true);
        token.getUser().setPasswordHash(passwordEncoder.encode(password));
        return new AuthDtos.MessageResponse("Your password has been updated. You can sign in.");
    }

    @Transactional
    public AuthDtos.MessageResponse change(String current, String next) {
        User user = users.findById(SecurityUtils.currentUserId()).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(current, user.getPasswordHash())) {
            throw new BusinessException("The current password is incorrect.");
        }
        validatePassword(next);
        user.setPasswordHash(passwordEncoder.encode(next));
        return new AuthDtos.MessageResponse("Password updated.");
    }

    @Transactional
    public User ensureUser(String email, String rawPassword, String name, String roleName, RoleRepository roles) {
        if (users.existsByEmailIgnoreCase(email)) {
            return users.findByEmailIgnoreCase(email).orElseThrow();
        }
        Role role = roles.findByName(roleName).orElseThrow(() -> new ConflictException("Role " + roleName + " is missing"));
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFullName(name);
        user.setActive(true);
        user.getRoles().add(role);
        return users.save(user);
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8 || !password.chars().anyMatch(Character::isLetter) || !password.chars().anyMatch(Character::isDigit)) {
            throw new BusinessException("Password must be at least 8 characters and include a letter and a number.");
        }
    }

    private String issueRefresh(User user, String ip, String userAgent) {
        String raw = TokenHashes.randomToken();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(TokenHashes.sha256(raw));
        token.setExpiresAt(Instant.now().plusMillis(properties.getJwt().getRefreshExpiration()));
        token.setIpAddress(ip);
        token.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 400)));
        refreshTokens.save(token);
        return raw;
    }

    private AuthDtos.TokenResponse tokens(StaffUserPrincipal principal, String refresh) {
        User user = users.findById(principal.getId()).orElseThrow();
        return new AuthDtos.TokenResponse(
                jwtService.accessToken(principal),
                refresh,
                properties.getJwt().getAccessExpiration() / 1000,
                profile(user)
        );
    }

    public AuthDtos.UserProfile profile(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AuthDtos.UserProfile(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.isActive(), roles, permissions, user.getLastLoginAt());
    }
}
