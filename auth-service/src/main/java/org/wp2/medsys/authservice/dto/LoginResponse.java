package org.wp2.medsys.authservice.dto;

import org.wp2.medsys.authservice.domain.Role;

public record LoginResponse(
        Long id,
        String username,
        String email,
        Role role,
        String token
) {}
