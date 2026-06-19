package com.karpenko.onlineshop.security;

import com.karpenko.onlineshop.entity.AuthProvider;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService defaultOAuth2UserService = new DefaultOAuth2UserService();
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = defaultOAuth2UserService.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email is required from OAuth2 provider");
        }

        String providerId = oauth2User.getAttribute("sub");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");

        User user = userRepository.findByEmail(email.toLowerCase().trim()).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email.toLowerCase().trim());
            newUser.setFirstName(firstName != null ? firstName : "Unknown");
            newUser.setLastName(lastName != null ? lastName : "Unknown");
            // Random password for OAuth users as they don't use password login
            newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            newUser.setRole(Role.USER);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setProviderId(providerId);
            log.info("Created new user via OAuth2: {}", email);
            return userRepository.save(newUser);
        });

        if (user.getProviderId() == null && providerId != null) {
            user.setProviderId(providerId);
            user.setAuthProvider(AuthProvider.GOOGLE);
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new DisabledException("Account is blocked");
        }

        return new CustomUserDetails(user, oauth2User.getAttributes());
    }
}