package com.karpenko.onlineshop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeDto {

    @NotBlank(message = "{validation.password.current.required}")
    private String currentPassword;

    @NotBlank(message = "{validation.password.new.required}")
    @Size(min = 8, message = "{validation.password.new.min}")
    private String newPassword;

    @NotBlank(message = "{validation.password.confirm.required}")
    private String confirmPassword;
}