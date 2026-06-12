package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.security.AuthSuccessHandler;
import com.karpenko.onlineshop.security.CustomOAuth2UserService;
import com.karpenko.onlineshop.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Zentrale Konfiguration für Spring Security.
 * Definiert die SecurityFilterChain, den PasswordEncoder und die Zugriffsregeln.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthSuccessHandler authSuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Öffentliche Ressourcen
                        .requestMatchers("/", "/register", "/login", "/error", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                        .requestMatchers("/favicon.ico", "/images/**", "/css/**", "/js/**", "/webjars/**", "/images/site.webmanifest").permitAll()
                        // Admin-Bereich: Nur für ROLE_ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Kundenbereich: Für alle authentifizierten Benutzer (USER und ADMIN)
                        .requestMatchers("/shop/**", "/profile/**").authenticated()
                        // Alle anderen Anfragen erfordern Authentifizierung
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(authSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}