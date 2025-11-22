package org.wp2.medsys.notificationservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT helper used by notification-service.
 *
 * It is intentionally compatible with auth-service JwtUtil:
 *  - Same secret (auth.jwt.secret)
 *  - Same algorithm (HMAC-SHA256)
 *  - Same claims: sub (username), "role"
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROLE = "role";

    private final Algorithm algorithm;

    public JwtUtil(@Value("${auth.jwt.secret}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    private DecodedJWT decode(String token) throws JWTVerificationException {
        return JWT
                .require(algorithm)
                .build()
                .verify(token);
    }

    public boolean isTokenValid(String token) {
        try {
            decode(token);
            return true;
        } catch (JWTVerificationException ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return decode(token).getSubject();
    }

    public String extractRole(String token) {
        return decode(token).getClaim(CLAIM_ROLE).asString();
    }
}
