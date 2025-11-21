package org.wp2.medsys.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.wp2.medsys.authservice.domain.Role;

import java.time.LocalDate;

public record RegisterDTO(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotNull(message = "Role is required")
        Role role
) {}
