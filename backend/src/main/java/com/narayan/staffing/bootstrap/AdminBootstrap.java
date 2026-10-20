package com.narayan.staffing.bootstrap;

import com.narayan.staffing.config.AppProperties;
import com.narayan.staffing.identity.AuthService;
import com.narayan.staffing.identity.RoleRepository;
import com.narayan.staffing.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final AppProperties properties;
    private final UserRepository users;
    private final RoleRepository roles;
    private final AuthService authService;

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        var bootstrap = properties.getBootstrap();
        if (bootstrap.getAdminEmail() == null || bootstrap.getAdminEmail().isBlank() || bootstrap.getAdminPassword() == null || bootstrap.getAdminPassword().isBlank()) {
            log.warn("No users exist and APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD are not set. Sign-in is unavailable until an admin is created.");
            return;
        }
        authService.ensureUser(bootstrap.getAdminEmail(), bootstrap.getAdminPassword(), bootstrap.getAdminName(), "SUPER_ADMIN", roles);
        log.info("Bootstrap admin created for {}", bootstrap.getAdminEmail());
    }
}
