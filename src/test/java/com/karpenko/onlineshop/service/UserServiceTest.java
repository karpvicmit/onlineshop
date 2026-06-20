package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.security.CustomUserDetails;
import com.karpenko.onlineshop.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Registration and User Management")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto validDto;

    @BeforeEach
    void setUp() {
        validDto = new UserRegistrationDto();
        validDto.setEmail("test@example.com");
        validDto.setPassword("SecurePass123");
        validDto.setFirstName("Max");
        validDto.setLastName("Mustermann");
        validDto.setAddress("Berlin, Str. 1");

        // Clear security context before each test
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("registerUser()")
    class Registration {

        @Test
        @DisplayName("Should register user with valid data, BCrypt hash, role USER")
        void shouldRegisterUserSuccessfully() {
            // given
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            // when
            User saved = userService.registerUser(validDto);

            // then
            assertThat(saved.getId()).isEqualTo(1L);
            assertThat(saved.getEmail()).isEqualTo("test@example.com");
            assertThat(saved.getRole()).isEqualTo(Role.USER);
            assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(saved.getPasswordHash()).isEqualTo("$2a$12$hashed");

            verify(passwordEncoder).encode("SecurePass123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should normalize email to lowercase and trim whitespace")
        void shouldNormalizeEmail() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            validDto.setEmail("  USER@Example.COM  ");

            userService.registerUser(validDto);

            verify(userRepository).save(argThat(user ->
                    user.getEmail().equals("user@example.com")));
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException on duplicate email")
        void shouldThrowWhenEmailExists() {
            when(passwordEncoder.encode(anyString())).thenReturn("hash");
            when(userRepository.save(any(User.class)))
                    .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

            assertThatThrownBy(() -> userService.registerUser(validDto))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("already registered");
        }
    }

    @Nested
    @DisplayName("updateUserStatus()")
    class StatusManagement {

        @Test
        @DisplayName("Admin should block active user")
        void shouldBlockUser() {
            // given
            User admin = buildUser(1L, "admin@test.de", Role.ADMIN);
            User target = buildUser(2L, "victim@test.de", Role.USER);
            mockAuthenticatedUser(admin);

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            userService.updateUserStatus(2L, UserStatus.BLOCKED);

            // then
            assertThat(target.getStatus()).isEqualTo(UserStatus.BLOCKED);
            verify(userRepository).save(target);
        }

        @Test
        @DisplayName("Admin should NOT be able to block themselves")
        void shouldPreventAdminFromBlockingThemselves() {
            User admin = buildUser(1L, "admin@test.de", Role.ADMIN);
            mockAuthenticatedUser(admin);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.updateUserStatus(1L, UserStatus.BLOCKED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("themselves");
        }

        @Test
        @DisplayName("Should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            User admin = buildUser(1L, "admin@test.de", Role.ADMIN);
            mockAuthenticatedUser(admin);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserStatus(99L, UserStatus.BLOCKED))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateUserRole()")
    class RoleManagement {

        @Test
        @DisplayName("Admin should change USER to ADMIN")
        void shouldChangeRole() {
            User admin = buildUser(1L, "admin@test.de", Role.ADMIN);
            User target = buildUser(2L, "user@test.de", Role.USER);
            mockAuthenticatedUser(admin);

            when(userRepository.findById(2L)).thenReturn(Optional.of(target));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.updateUserRole(2L, Role.ADMIN);

            assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("Admin cannot change their own role")
        void shouldPreventSelfRoleChange() {
            User admin = buildUser(1L, "admin@test.de", Role.ADMIN);
            mockAuthenticatedUser(admin);
            when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.updateUserRole(1L, Role.USER))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========= Helper methods =========

    private User buildUser(Long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private void mockAuthenticatedUser(User user) {
        CustomUserDetails principal = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}