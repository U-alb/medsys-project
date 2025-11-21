package org.wp2.medsys.authservice.dto;

import org.wp2.medsys.authservice.domain.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        Role role
) {}
