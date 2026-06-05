package com.karpenko.onlineshop.security;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementiert UserDetailsService, um Benutzerdaten während der Authentifizierung aus der Datenbank zu laden.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Versuch, Benutzer mit E-Mail {} zu laden", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Benutzer mit E-Mail {} nicht gefunden", email);
                    return new UsernameNotFoundException("Benutzer nicht gefunden: " + email);
                });

        return new CustomUserDetails(user);
    }
}