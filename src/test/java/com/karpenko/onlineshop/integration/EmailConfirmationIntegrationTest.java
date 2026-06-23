package com.karpenko.onlineshop.integration;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Email Confirmation Integration Tests")
class EmailConfirmationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("Registration should create user with unconfirmed email and generate token")
    void registrationShouldCreateUnconfirmedUser() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("newuser@test.de");
        dto.setPassword("SecurePass123");
        dto.setFirstName("New");
        dto.setLastName("User");
        dto.setAddress("Test Address");

        User user = userService.registerUser(dto);

        assertThat(user.getEmailConfirmationToken()).isNotNull();
        assertThat(user.isEmailConfirmed()).isFalse();
        assertThat(user.getTokenExpiryDate()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Email confirmation should activate user account")
    void emailConfirmationShouldActivateUser() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("confirm@test.de");
        dto.setPassword("SecurePass123");
        dto.setFirstName("Confirm");
        dto.setLastName("Test");
        dto.setAddress("Test Address");

        User user = userService.registerUser(dto);
        String token = user.getEmailConfirmationToken();

        boolean result = userService.confirmEmail(token);

        assertThat(result).isTrue();

        User confirmedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(confirmedUser.isEmailConfirmed()).isTrue();
        assertThat(confirmedUser.getEmailConfirmationToken()).isNull();
        assertThat(confirmedUser.getTokenExpiryDate()).isNull();
    }

    @Test
    @DisplayName("Invalid token should throw exception")
    void invalidTokenShouldThrowException() {
        assertThatThrownBy(() -> userService.confirmEmail("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid confirmation token");
    }

    @Test
    @DisplayName("Expired token should throw exception")
    void expiredTokenShouldThrowException() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("expired@test.de");
        dto.setPassword("SecurePass123");
        dto.setFirstName("Expired");
        dto.setLastName("Test");
        dto.setAddress("Test Address");

        User user = userService.registerUser(dto);

        // Simulating token expiration
        user.setTokenExpiryDate(LocalDateTime.now().minusHours(1));
        userRepository.save(user);

        assertThatThrownBy(() -> userService.confirmEmail(user.getEmailConfirmationToken()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Token should be single-use")
    void tokenShouldBeSingleUse() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("singleuse@test.de");
        dto.setPassword("SecurePass123");
        dto.setFirstName("Single");
        dto.setLastName("Use");
        dto.setAddress("Test Address");

        User user = userService.registerUser(dto);
        String token = user.getEmailConfirmationToken();

        userService.confirmEmail(token);

        assertThatThrownBy(() -> userService.confirmEmail(token))
                .isInstanceOf(IllegalArgumentException.class);
    }
}