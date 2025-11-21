package org.wp2.medsys.authservice.factory;

import org.springframework.stereotype.Component;
import org.wp2.medsys.authservice.domain.Doctor;
import org.wp2.medsys.authservice.domain.Patient;
import org.wp2.medsys.authservice.domain.Role;
import org.wp2.medsys.authservice.domain.User;
import org.wp2.medsys.authservice.dto.RegisterDTO;

/**
 * Factory Method: builds a concrete subclass (Patient or Doctor) from RegisterDTO.
 * POC defaults:
 *  - Doctor: spec = "General", licenseNumber = null
 *  - Patient: optional fields left null for now
 */
@Component
public class RegistrationFactory {

    public User create(RegisterDTO dto) {
        if (dto.role() == Role.DOCTOR) {
            Doctor d = new Doctor();
            d.setUsername(dto.username());
            d.setEmail(dto.email());
            d.setDateOfBirth(dto.dateOfBirth());
            d.setSpec("General");
            d.setLicenseNumber(null);
            d.setRole(Role.DOCTOR);
            return d;
        } else {
            // Treat anything else as PATIENT for now (PATIENT is default)
            Patient p = new Patient();
            p.setUsername(dto.username());
            p.setEmail(dto.email());
            p.setDateOfBirth(dto.dateOfBirth());
            p.setGender(null);
            p.setPhoneNumber(null);
            p.setAddress(null);
            p.setRole(Role.PATIENT);
            return p;
        }
    }
}
