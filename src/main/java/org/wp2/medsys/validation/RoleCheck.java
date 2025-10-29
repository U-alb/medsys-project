package org.wp2.medsys.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wp2.medsys.domain.Appointment;

@Slf4j
public class RoleCheck extends BaseValidationHandler {

    @Override
    protected void doValidate(Appointment appointment) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (appointment.getPatient() == null || appointment.getDoctor() == null) {
            log.info("[CoR] RoleCheck -> FAIL (missing patient/doctor)");
            throw new Throwable("Patient and Doctor must be provided.");
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));

        if (!isPatient) {
            log.info("[CoR] RoleCheck -> FAIL (not PATIENT)");
            throw new Throwable("Only patients can schedule appointments.");
        }

        // Identity consistency: the logged-in username must match the patient in the request
        String loggedUser = auth.getName();
        String patientUsername = appointment.getPatient().getUsername();
        if (patientUsername == null || !patientUsername.equals(loggedUser)) {
            log.info("[CoR] RoleCheck -> FAIL (patient mismatch) logged={}, patient={}", loggedUser, patientUsername);
            throw new Throwable("You can only schedule for your own account.");
        }

        log.info("[CoR] RoleCheck -> OK user={} (PATIENT)", loggedUser);
    }
}
