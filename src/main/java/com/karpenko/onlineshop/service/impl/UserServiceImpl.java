package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.security.CustomUserDetails;
import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.List;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Проверяем, что пользователь действительно аутентифицирован и не аноним
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Benutzer ist nicht authentifiziert");
        }

        // Извлекаем нашего CustomUserDetails (теперь он будет и для Form, и для OAuth2)
        if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUser();
        }

        throw new IllegalStateException("Unbekannter Principal-Typ: " + authentication.getPrincipal().getClass().getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Benutzer nicht gefunden"));
        Long currentUserId = getCurrentUserId();

        if (user.getId().equals(currentUserId)) {
            throw new IllegalStateException("Ein Administrator kann seine eigene Rolle nicht ändern.");
        }

        user.setRole(newRole);
        userRepository.save(user);
        log.info("Rolle von Benutzer {} auf {} geändert", user.getEmail(), newRole);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Benutzer nicht gefunden"));
        Long currentUserId = getCurrentUserId();

        if (user.getId().equals(currentUserId)) {
            throw new IllegalStateException("Ein Administrator kann sich nicht selbst sperren oder entsperren.");
        }
        user.setStatus(newStatus);
        userRepository.save(user);
        log.info("Status von Benutzer {} auf {} geändert", user.getEmail(), newStatus);
    }

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Kein authentifizierter Benutzer im Sicherheitskontext gefunden.");
        }

        CustomUserDetails currentUserDetails = (CustomUserDetails) authentication.getPrincipal();
        return currentUserDetails.getId();
    }
}
