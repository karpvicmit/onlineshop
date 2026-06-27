package com.karpenko.onlineshop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordChangeDto {

    @NotBlank(message = "Aktuelles Passwort darf nicht leer sein")
    private String currentPassword;

    @NotBlank(message = "Neues Passwort darf nicht leer sein")
    @Size(min = 8, message = "Neues Passwort muss mindestens 8 Zeichen lang sein")
    private String newPassword;

    @NotBlank(message = "Passwort-Bestätigung darf nicht leer sein")
    private String confirmPassword;
}