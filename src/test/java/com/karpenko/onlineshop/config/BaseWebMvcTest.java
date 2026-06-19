package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.security.AuthSuccessHandler;
import com.karpenko.onlineshop.security.CustomOAuth2UserService;
import com.karpenko.onlineshop.security.CustomUserDetailsService;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Base class for all @WebMvcTest tests.
 * Explicitly imports SecurityConfig and PasswordEncoderConfig
 * so that the SecurityFilterChain is actually applied during tests.
 * Without this, @WebMvcTest skips all URL-based role checks.
 */
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
public abstract class BaseWebMvcTest {

    @MockitoBean
    protected CustomUserDetailsService userDetailsService;

    @MockitoBean
    protected AuthSuccessHandler authSuccessHandler;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

}