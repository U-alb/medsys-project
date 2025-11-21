package org.wp2.medsys.authservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("DOCTOR")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Doctor extends User {

    @Column(length = 100)
    private String spec;

    @Column(name = "license_number", length = 50, unique = true)
    private String licenseNumber;

    /** Shared NOT-NULL column for all rows in the users table */
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
}
