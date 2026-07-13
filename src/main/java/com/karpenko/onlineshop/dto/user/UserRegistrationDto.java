package com.karpenko.onlineshop.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object for user registration.
 * Validation messages use keys from ValidationMessages.properties for i18n.
 */
@Data
public class UserRegistrationDto {

    @NotBlank(message = "{validation.user.email.required}")
    @Email(message = "{validation.user.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.user.password.required}")
    @Size(min = 8, message = "{validation.user.password.min}")
    private String password;

    private String firstName;

    private String lastName;

    @NotBlank(message = "{validation.user.address.required}")
    private String address;
}