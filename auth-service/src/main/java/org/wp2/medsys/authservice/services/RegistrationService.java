package org.wp2.medsys.authservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wp2.medsys.authservice.domain.User;
import org.wp2.medsys.authservice.dto.RegisterDTO;
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
        // Could add uniqueness checks here if you want nicer errors.
        User user = factory.create(dto);
        user.setPassHash(encoder.encode(dto.password()));

        try {
            return repo.save(user);
        } catch (DataIntegrityViolationException ex) {
            // For now, just rethrow – we'll refine error handling later.
            throw ex;
        }
    }
}
