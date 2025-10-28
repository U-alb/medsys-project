package org.wp2.medsys.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wp2.medsys.DTO.RegisterDTO;
import org.wp2.medsys.domain.User;
import org.wp2.medsys.factory.RegistrationFactory;
import org.wp2.medsys.repositories.UserRepository;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final RegistrationFactory factory;

    @Transactional
    public User register(RegisterDTO dto) {
        User user = factory.create(dto);
        user.setPassHash(encoder.encode(dto.password()));
        return repo.save(user);
    }
}
