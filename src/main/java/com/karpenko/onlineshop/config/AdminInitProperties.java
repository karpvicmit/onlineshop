package com.karpenko.onlineshop.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.init.admin")
@Getter
@Setter
public class AdminInitProperties {
    private String email;
    private String password;
}