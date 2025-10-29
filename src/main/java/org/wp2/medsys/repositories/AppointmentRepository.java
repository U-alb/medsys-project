package org.wp2.medsys.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wp2.medsys.domain.Appointment;

import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Number of appointments for a doctor at an exact time slot.
     * (Our POC treats appointmentDate as a single time slot without duration.)
     */
    long countByDoctorIdAndAppointmentDate(Long doctorId, LocalDateTime appointmentDate);
}
