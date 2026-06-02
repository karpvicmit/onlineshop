package com.ibb.onlineshop.service.impl;

import com.ibb.onlineshop.security.CustomUserDetails;
import com.ibb.onlineshop.dto.user.UserRegistrationDto;
import com.ibb.onlineshop.entity.User;
import com.ibb.onlineshop.entity.Role;
import com.ibb.onlineshop.entity.UserStatus;
import com.ibb.onlineshop.exception.EmailAlreadyExistsException;
import com.ibb.onlineshop.repository.UserRepository;
import com.ibb.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementiert die Geschäftslogik für die Benutzerverwaltung.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        log.info("Registrierungsversuch für E-Mail: {}", dto.getEmail());

        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registrierung fehlgeschlagen: E-Mail {} ist bereits registriert", dto.getEmail());
            throw new EmailAlreadyExistsException("Diese E-Mail-Adresse ist bereits registriert.");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        // Passwort wird mit BCrypt (Stärke 12) gehasht
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAddress(dto.getAddress());

        User savedUser = userRepository.save(user);
        log.info("Benutzer {} erfolgreich registriert", savedUser.getEmail());

        return savedUser;
    }

    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser();
        }
        throw new IllegalStateException("Benutzer ist nicht authentifiziert");
    }
}
