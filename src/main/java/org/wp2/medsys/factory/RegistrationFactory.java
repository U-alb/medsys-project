package org.wp2.medsys.factory;

import org.springframework.stereotype.Component;
import org.wp2.medsys.DTO.RegisterDTO;
import org.wp2.medsys.domain.*;

import java.time.LocalDateTime;

/**
 * Factory Method: builds a concrete subclass (Patient or Doctor) from the RegisterDTO.
 * POC defaults:
 *  - Doctor: spec = "General", licenseNumber = null (can be filled later by admin)
 *  - Patient: optional fields left null for now
 */
@Component
public class RegistrationFactory {

    public User create(RegisterDTO dto) {
        if (dto.role() == Role.DOCTOR) {
            Doctor d = new Doctor();
            // base fields
            d.setUsername(dto.username());
            d.setEmail(dto.email());
            d.setCreatedAt(LocalDateTime.now());
            d.setRole(Role.DOCTOR);

            // shared NOT-NULL column on users table
            d.setDateOfBirth(dto.dateOfBirth());

            // doctor-specific (POC defaults)
            d.setSpec("General");
            d.setLicenseNumber(null); // keep null in POC; unique allows multiple NULLs in MySQL/MariaDB

            return d;
        } else {
            // default: PATIENT
            Patient p = new Patient();
            // base fields
            p.setUsername(dto.username());
            p.setEmail(dto.email());
            p.setCreatedAt(LocalDateTime.now());
            p.setRole(Role.PATIENT);

            // shared NOT-NULL column on users table
            p.setDateOfBirth(dto.dateOfBirth());

            // patient-specific (optional in POC)
            p.setGender(null);
            p.setPhoneNumber(null);
            p.setAddress(null);

            return p;
        }
    }
}
