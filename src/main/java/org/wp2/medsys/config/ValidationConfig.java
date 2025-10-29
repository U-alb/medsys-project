package org.wp2.medsys.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wp2.medsys.repositories.AppointmentRepository;
import org.wp2.medsys.repositories.DoctorRepository;
import org.wp2.medsys.validation.*;

@Slf4j
@Configuration
public class ValidationConfig {

    @Bean
    public ValidationHandler bookingValidationChain(DoctorRepository doctorRepository,
                                                    AppointmentRepository appointmentRepository) {
        ValidationHandler auth = new AuthCheck();
        ValidationHandler role = new RoleCheck();
        ValidationHandler doc  = new DoctorAvailabilityCheck(doctorRepository);
        ValidationHandler pat  = new PatientConflictCheck(appointmentRepository);
        ValidationHandler biz  = new BusinessRulesCheck(appointmentRepository);

        auth.setNext(role);
        role.setNext(doc);
        doc.setNext(pat);
        pat.setNext(biz);

        log.info("Booking validation CoR initialized: Auth -> Role -> DoctorAvailability -> PatientConflict -> BusinessRules");
        return auth;
    }
}
