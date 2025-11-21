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

@Component
public class JwtUtil {

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
                .withClaim("role", role.name())
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
        String roleName = decodeToken(token).getClaim("role").asString();
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
