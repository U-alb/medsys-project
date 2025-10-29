package org.wp2.medsys.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.errors.UnauthorizedException;

@Slf4j
public class AuthCheck extends BaseValidationHandler {
    @Override
    protected void doValidate(Appointment appointment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.info("[CoR] AuthCheck -> FAIL (unauthenticated)");
            throw new UnauthorizedException("You must be logged in.");
        }
        log.info("[CoR] AuthCheck -> OK user={}", auth.getName());
    }
}
