package org.wp2.medsys.authservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.wp2.medsys.authservice.domain.User;
import org.wp2.medsys.authservice.dto.LoginRequest;
import org.wp2.medsys.authservice.dto.LoginResponse;
import org.wp2.medsys.authservice.exceptions.InvalidCredentialsException;
import org.wp2.medsys.authservice.repositories.UserRepository;
import org.wp2.medsys.authservice.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // Find user by username
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPassHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Generate JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        // Build response
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                token
        );
    }
}
