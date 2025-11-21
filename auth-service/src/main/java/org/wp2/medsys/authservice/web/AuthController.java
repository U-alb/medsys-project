package org.wp2.medsys.authservice.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.authservice.domain.User;
import org.wp2.medsys.authservice.dto.LoginRequest;
import org.wp2.medsys.authservice.dto.LoginResponse;
import org.wp2.medsys.authservice.dto.RegisterDTO;
import org.wp2.medsys.authservice.dto.UserResponse;
import org.wp2.medsys.authservice.exceptions.InvalidCredentialsException;
import org.wp2.medsys.authservice.exceptions.UserAlreadyExistsException;
import org.wp2.medsys.authservice.repositories.UserRepository;
import org.wp2.medsys.authservice.services.AuthService;
import org.wp2.medsys.authservice.services.RegistrationService;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO dto) {
        try {
            User saved = registrationService.register(dto);

            UserResponse response = new UserResponse(
                    saved.getId(),
                    saved.getUsername(),
                    saved.getEmail(),
                    saved.getRole()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", 409,
                            "error", "Conflict",
                            "message", ex.getMessage()
                    ));
        } catch (IllegalArgumentException ex) {
            // e.g. trying to register as ADMIN
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "error", "Bad Request",
                            "message", ex.getMessage()
                    ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (InvalidCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Invalid username or password"
                    ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        String username = (String) authentication.getPrincipal();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }
}
