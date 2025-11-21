package org.wp2.medsys.authservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wp2.medsys.authservice.domain.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
