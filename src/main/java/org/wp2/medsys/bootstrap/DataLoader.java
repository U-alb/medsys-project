// src/main/java/org/wp2/medsys/bootstrap/DataLoader.java
package org.wp2.medsys.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.wp2.medsys.domain.*;
import org.wp2.medsys.services.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@Profile("dev") // <-- Seeder runs only under spring.profiles.active=dev
public class DataLoader implements CommandLineRunner {

    private final PatientService       patientService;
    private final DoctorService        doctorService;
    private final AppointmentService   appointmentService;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService  prescriptionService;
    private final PasswordEncoder      passwordEncoder;

    public DataLoader(PatientService patientService,
                      DoctorService doctorService,
                      AppointmentService appointmentService,
                      MedicalRecordService medicalRecordService,
                      PrescriptionService prescriptionService,
                      PasswordEncoder passwordEncoder) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.medicalRecordService = medicalRecordService;
        this.prescriptionService = prescriptionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        /* -------- 1) clean slate (children → parents) -------- */
        appointmentService.deleteAll();
        medicalRecordService.deleteAll();
        prescriptionService.deleteAll();
        patientService.deleteAll();
        doctorService.deleteAll();

        /* -------- 2) patients -------- */
        Patient john = new Patient(
                "john.doe",
                "john.doe@example.com",
                passwordEncoder.encode("pass"),
                LocalDate.of(1990, 1, 1),
                "M",
                "0722-123-456",
                "123 Main St, Springfield"
        );

        Patient jane = new Patient(
                "jane.roe",
                "jane.roe@example.com",
                passwordEncoder.encode("pass"),
                LocalDate.of(1992, 6, 15),
                "F",
                "0722-654-321",
                "456 Elm St, Springfield"
        );

        john = patientService.create(john);
        jane = patientService.create(jane);

        /* -------- 3) doctors -------- */
        Doctor house = new Doctor(
                "house",
                "house@example.com",
                passwordEncoder.encode("pass"),
                LocalDate.of(1970, 5, 15),
                "Diagnostics",
                "DOC-1001"
        );

        Doctor wattson = new Doctor(
                "wattson",
                "wattson@example.com",
                passwordEncoder.encode("pass"),
                LocalDate.of(1968, 2, 13),
                "Diagnostics",
                "DOC-1002"
        );

        house   = doctorService.create(house);
        wattson = doctorService.create(wattson);

        /* -------- 4) appointments (relative to 'now') -------- */
        LocalDateTime t1 = LocalDateTime.now()
                .plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0);
        LocalDateTime t2 = LocalDateTime.now()
                .plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);

        Appointment ap1 = new Appointment(
                t1,
                john,
                house,
                "Annual physical exam",
                Status.ACCEPTED
        );

        Appointment ap2 = new Appointment(
                t2,
                jane,
                wattson,
                "Flu-like symptoms",
                Status.PENDING
        );

        appointmentService.create(ap1);
        appointmentService.create(ap2);

        /* -------- 5) medical records -------- */
        MedicalRecord mr1 = new MedicalRecord(
                "Blood work",
                "All values within normal ranges.",
                john,
                house,
                LocalDateTime.now().minusMonths(1).withSecond(0).withNano(0)
        );

        medicalRecordService.create(mr1);

        /* -------- 6) prescriptions -------- */
        Prescription rx1 = new Prescription(
                LocalDate.now(),
                "Ibuprofen",
                "200 mg twice a day after meals",
                john,
                house,
                null
        );

        prescriptionService.create(rx1);

        /* -------- 7) confirm -------- */
        log.info("=== Sample Data Loaded (dev) ===");
        log.info("Patients:       {}", patientService.findAll().size());
        log.info("Doctors:        {}", doctorService.findAll().size());
        log.info("Appointments:   {}", appointmentService.findAll().size());
        log.info("MedicalRecords: {}", medicalRecordService.findAll().size());
        log.info("Prescriptions:  {}", prescriptionService.findAll().size());
    }
}
