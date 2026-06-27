package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.user.PasswordChangeDto;
import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.AuthProvider;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.security.CustomUserDetails;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public User registerUser(UserRegistrationDto dto) {
        log.info("Registration attempt for email: {}", dto.getEmail());
        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAddress(dto.getAddress());

        String token = UUID.randomUUID().toString();
        user.setEmailConfirmationToken(token);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
        user.setEmailConfirmed(false);

        try {
            User savedUser = userRepository.save(user);
            log.info("User {} registered successfully, sending confirmation email", savedUser.getEmail());
            emailService.sendConfirmationEmail(savedUser.getEmail(), token, baseUrl);
            return savedUser;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Registration failed: email {} already exists", dto.getEmail());
            throw new EmailAlreadyExistsException("This email is already registered.");
        }
    }

    @Override
    @Transactional
    public boolean confirmEmail(String token) {
        log.info("Email confirmation attempt with token");
        User user = userRepository.findByEmailConfirmationToken(token)
                .orElseThrow(() -> {
                    log.warn("Invalid confirmation token");
                    return new IllegalArgumentException("Invalid confirmation token");
                });

        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Confirmation token expired for user: {}", user.getEmail());
            throw new IllegalStateException("Confirmation token has expired");
        }

        user.setEmailConfirmed(true);
        user.setEmailConfirmationToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);

        log.info("Email confirmed successfully for user: {}", user.getEmail());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("User is not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails customUserDetails)) {
            throw new IllegalStateException("Unknown principal type: " + principal.getClass().getName());
        }
        Long userId = customUserDetails.getId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User no longer exists"));
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        log.warn("Calling deprecated getAllUsers() without pagination - use with caution on large datasets.");
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Fetching users with pagination: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("No authenticated user found in security context.");
        }
        CustomUserDetails currentUserDetails = (CustomUserDetails) authentication.getPrincipal();
        return currentUserDetails.getId();
    }

    @Override
    @Transactional
    public void updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long currentUserId = getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new IllegalStateException("Admin cannot change their own role.");
        }

        user.setRole(newRole);
        userRepository.save(user);
        log.info("Role of user {} changed to {}", user.getEmail(), newRole);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UserStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long currentUserId = getCurrentUserId();
        if (user.getId().equals(currentUserId)) {
            throw new IllegalStateException("Admin cannot block/unblock themselves.");
        }

        user.setStatus(newStatus);
        userRepository.save(user);
        log.info("Status of user {} changed to {}", user.getEmail(), newStatus);
    }

    @Override
    @Transactional
    public void changePassword(User currentUser, PasswordChangeDto dto) {
        log.info("Password change attempt for user: {}", currentUser.getEmail());

        if (currentUser.getAuthProvider() != AuthProvider.LOCAL) {
            log.warn("Password change rejected for OAuth2 user: {}", currentUser.getEmail());
            throw new IllegalStateException(
                    "Passwort kann nicht geändert werden. Sie sind über einen externen Anbieter angemeldet.");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), currentUser.getPasswordHash())) {
            log.warn("Invalid current password for user: {}", currentUser.getEmail());
            throw new IllegalArgumentException("Das aktuelle Passwort ist falsch.");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            log.warn("Password mismatch for user: {}", currentUser.getEmail());
            throw new IllegalArgumentException("Die neuen Passwörter stimmen nicht überein.");
        }

        currentUser.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(currentUser);

        // Send email notification
        try {
            emailService.sendPasswordChangeNotification(currentUser.getEmail(), LocalDateTime.now());
            log.info("Password change notification email sent to: {}", currentUser.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send password change notification email: {}", e.getMessage());
        }

        log.info("Password changed successfully for user: {}", currentUser.getEmail());
    }
}