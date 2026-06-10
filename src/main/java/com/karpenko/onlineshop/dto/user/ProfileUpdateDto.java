package com.karpenko.onlineshop.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateDto {
    @NotBlank(message = "Vorname darf nicht leer sein")
    private String firstName;

    @NotBlank(message = "Nachname darf nicht leer sein")
    private String lastName;

    @NotBlank(message = "Lieferadresse darf nicht leer sein")
    private String address;
}