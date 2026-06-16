package com.karpenko.onlineshop.security;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.AuthProvider;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
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

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");

        log.debug("OAuth2 Login versucht für E-Mail: {}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Neuer Benutzer wird über OAuth2 registriert: {}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName != null ? firstName : "Unknown");
            newUser.setLastName(lastName != null ? lastName : "Unknown");
            newUser.setPasswordHash(UUID.randomUUID().toString());
            newUser.setRole(Role.USER);
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setAuthProvider(AuthProvider.GOOGLE);

            return userRepository.save(newUser);
        });

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("VERSUCHTER LOGIN: Gesperrter Benutzer versucht sich via OAuth2 anzumelden: {}", email);
            throw new DisabledException("Dieses Benutzerkonto wurde gesperrt. Bitte kontaktieren Sie den Administrator.");
        }

        return new CustomUserDetails(user, oauth2User.getAttributes());
    }
}