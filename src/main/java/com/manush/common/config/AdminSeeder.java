package com.manush.common.config;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Seeds the ADMIN account ONCE when ADMIN_SEED_ENABLED=true.
 * Disable immediately after first run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${manush.admin.seed-enabled:false}")
    private boolean seedEnabled;

    @Value("${manush.admin.seed-email:admin@manush.app}")
    private String adminEmail;

    @Value("${manush.admin.seed-password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) return;
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_SEED_ENABLED=true but ADMIN_SEED_PASSWORD is blank — skipping seed");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists: {}", adminEmail);
            return;
        }
        User admin = User.builder()
                .email(adminEmail)
                .name("Admin")
                .passwordHash(passwordEncoder.encode(adminPassword))
                .roles(Set.of(Role.ADMIN))
                .emailVerified(true)
                .profileComplete(true)
                .active(true)
                .banned(false)
                .helpingPoints(0)
                .bloodGroup("O+")
                .build();
        userRepository.save(admin);
        log.info("Admin account seeded: {} — disable ADMIN_SEED_ENABLED now!", adminEmail);
    }
}
