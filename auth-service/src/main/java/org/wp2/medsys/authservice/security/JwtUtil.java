package org.wp2.medsys.authservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wp2.medsys.authservice.domain.Role;

import java.time.Instant;
import java.util.Date;

/**
 * Central JWT helper for MedSys.
 *
 * Token format (used across all microservices):
 *  - Algorithm: HMAC-SHA256
 *  - Subject (sub): username
 *  - Claim "role": one of PATIENT, DOCTOR, ADMIN
 *  - iat / exp: issued-at and expiration timestamps
 *
 * Other services (appointments, notifications, etc.) must:
 *  - use the same secret (auth.jwt.secret)
 *  - read "sub" as username
 *  - read "role" claim to apply authorization rules.
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROLE = "role";

    private final Algorithm algorithm;
    private final long expirationSeconds;

    public JwtUtil(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.expiration-seconds}") long expirationSeconds
    ) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(String username, Role role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return JWT.create()
                .withSubject(username)
                .withClaim(CLAIM_ROLE, role.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public DecodedJWT decodeToken(String token) throws JWTVerificationException {
        return JWT.require(algorithm)
                .build()
                .verify(token);
    }

    public String getUsername(String token) {
        return decodeToken(token).getSubject();
    }

    public Role getRole(String token) {
        String roleName = decodeToken(token).getClaim(CLAIM_ROLE).asString();
        return Role.valueOf(roleName);
    }

    public boolean isTokenValid(String token) {
        try {
            decodeToken(token);
            return true;
        } catch (JWTVerificationException ex) {
            return false;
        }
    }
}
