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

/**
 * Initialisiert die Anwendung mit einem Standard-Administrator,
 * falls noch kein Admin-Account in der Datenbank existiert.
 */
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
            log.warn("Kein Administrator gefunden. Erstelle Standard-Admin mit den konfigurierten Daten...");

            User admin = new User();
            admin.setEmail(adminProperties.getEmail());
            admin.setPasswordHash(passwordEncoder.encode(adminProperties.getPassword()));
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setAddress("Admin Street 1, 12345 Berlin");

            userRepository.save(admin);

            log.warn("=================================================================");
            log.warn("Standard-Admin erfolgreich erstellt!");
            log.warn("E-Mail: {}", adminProperties.getEmail());
            log.warn("BITTE ÄNDERN SIE DAS PASSWORT NACH DEM ERSTEN LOGIN!");
            log.warn("=================================================================");
        } else {
            log.debug("Mindestens ein Administrator ist bereits vorhanden. Initialisierung übersprungen.");
        }
    }
}