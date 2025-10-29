package org.wp2.medsys.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"doctor_id","appointment_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"patient","doctor"})
@ToString(exclude = {"patient","doctor"})
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @Column(name = "schedule_reason", length = 255)
    private String scheduleReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;   // default removes NOT-NULL violation

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    // Proper constructors for Appointment
    public Appointment(LocalDateTime appointmentDate,
                       Patient       patient,
                       Doctor        doctor,
                       String        scheduleReason,
                       Status        status) {
        this.appointmentDate = appointmentDate;
        this.patient         = patient;
        this.doctor          = doctor;
        this.scheduleReason  = scheduleReason;
        this.status          = (status == null) ? Status.PENDING : status;
    }

    //overloading in case the status is PENDING
    public Appointment(LocalDateTime appointmentDate,
                       Patient       patient,
                       Doctor        doctor,
                       String        scheduleReason) {
        this(appointmentDate, patient, doctor, scheduleReason, Status.PENDING);
    }


}
