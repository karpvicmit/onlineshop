package com.karpenko.onlineshop.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateDto {

    @NotBlank(message = "{validation.profile.firstName.required}")
    private String firstName;

    @NotBlank(message = "{validation.profile.lastName.required}")
    private String lastName;

    @NotBlank(message = "{validation.profile.address.required}")
    private String address;
}