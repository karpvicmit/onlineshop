package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminInitProperties adminProperties;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            String adminEmail = adminProperties.getEmail();
            String adminPassword = adminProperties.getPassword();

            if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                log.warn("Admin email or password not configured. Skipping admin creation.");
                return;
            }

            if (adminPassword.length() < 8) {
                log.warn("Admin password too short (min 8 characters). Skipping admin creation.");
                return;
            }

            log.warn("No admin found. Creating default administrator...");
            User admin = new User();
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setAddress("Admin Street 1, 12345 Berlin");

            userRepository.save(admin);

            log.warn("=================================================================");
            log.warn("Default administrator created successfully!");
            log.warn("Email: {}", adminEmail);
            log.warn("PLEASE CHANGE THE PASSWORD AFTER FIRST LOGIN!");
            log.warn("=================================================================");
        } else {
            log.debug("Admin user already exists. Initialization skipped.");
        }
    }
}