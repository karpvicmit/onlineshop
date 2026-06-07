package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");

        if (email == null || email.isEmpty()) {
            log.error("Keine E-Mail-Adresse vom OAuth2-Provider erhalten.");
            throw new OAuth2AuthenticationException("E-Mail-Adresse ist erforderlich");
        }

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            log.info("Neuer Benutzer wird über OAuth2 registriert: {}", email);
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPasswordHash("$2a$12$RandomHashForOAuth2UserOnlyNotUsedForAuth");
            newUser.setFirstName(oAuth2User.getAttribute("given_name"));
            newUser.setLastName(oAuth2User.getAttribute("family_name"));
            newUser.setRole(Role.USER);
            newUser.setStatus(UserStatus.ACTIVE);

            userRepository.save(newUser);
        } else {
            log.info("Bestehender Benutzer hat sich über OAuth2 angemeldet: {}", email);
            if (existingUser.get().getStatus() == UserStatus.BLOCKED) {
                throw new OAuth2AuthenticationException("Dieses Konto wurde gesperrt.");
            }
        }

        return oAuth2User;
    }
}
