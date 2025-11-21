package org.wp2.medsys.authservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wp2.medsys.authservice.domain.Role;
import org.wp2.medsys.authservice.domain.User;
import org.wp2.medsys.authservice.dto.RegisterDTO;
import org.wp2.medsys.authservice.exceptions.UserAlreadyExistsException;
import org.wp2.medsys.authservice.factory.RegistrationFactory;
import org.wp2.medsys.authservice.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final RegistrationFactory factory;

    @Transactional
    public User register(RegisterDTO dto) {
        // 1) Block self-registration as ADMIN
        if (dto.role() == Role.ADMIN) {
            throw new IllegalArgumentException("Self-registration as ADMIN is not allowed");
        }

        // 2) Check duplicates before hitting the DB constraint
        repo.findByUsername(dto.username()).ifPresent(existing -> {
            throw new UserAlreadyExistsException("Username already exists");
        });

        repo.findByEmail(dto.email()).ifPresent(existing -> {
            throw new UserAlreadyExistsException("Email already exists");
        });

        // 3) Build entity + hash password
        User user = factory.create(dto);
        user.setPassHash(encoder.encode(dto.password()));

        return repo.save(user);
    }
}
