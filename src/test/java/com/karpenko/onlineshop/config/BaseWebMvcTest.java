package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.security.AuthSuccessHandler;
import com.karpenko.onlineshop.security.CustomOAuth2UserService;
import com.karpenko.onlineshop.security.CustomUserDetailsService;
import com.karpenko.onlineshop.util.MessageUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({SecurityConfig.class, PasswordEncoderConfig.class})
@ActiveProfiles("test")
public abstract class BaseWebMvcTest {

    @MockitoBean
    protected CustomUserDetailsService userDetailsService;

    @MockitoBean
    protected AuthSuccessHandler authSuccessHandler;

    @MockitoBean
    protected CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    protected MessageUtil messageUtil;


}