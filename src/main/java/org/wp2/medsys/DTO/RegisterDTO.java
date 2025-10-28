package org.wp2.medsys.DTO;

import org.wp2.medsys.domain.Role;

import java.time.LocalDate;

public record RegisterDTO(
        String username,
        String email,
        String password,
        LocalDate dateOfBirth,
        Role role
) {}
