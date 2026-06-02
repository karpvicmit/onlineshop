package com.ibb.onlineshop.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object für die Benutzerregistrierung.
 * Enthält Validierungsannotationen für sichere Eingabedaten.
 */
@Data
public class UserRegistrationDto {

    @NotBlank(message = "E-Mail darf nicht leer sein")
    @Email(message = "Ungültiges E-Mail-Format")
    private String email;

    @NotBlank(message = "Passwort darf nicht leer sein")
    @Size(min = 8, message = "Passwort muss mindestens 8 Zeichen lang sein")
    private String password;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Lieferadresse darf nicht leer sein")
    private String address;
}
