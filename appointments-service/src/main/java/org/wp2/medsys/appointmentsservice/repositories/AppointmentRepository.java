package org.wp2.medsys.appointmentsservice.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.domain.Status;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientUsername(String patientUsername);

    List<Appointment> findByDoctorUsername(String doctorUsername);

    /**
     * Count doctor appointments that overlap a given interval and are in one of the given statuses.
     * Overlap rule: start1 < end2 AND end1 > start2
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.doctorUsername = :doctorUsername
              AND a.startTime < :end
              AND a.endTime > :start
              AND a.status IN :statuses
            """)
    long countOverlappingForDoctor(@Param("doctorUsername") String doctorUsername,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   @Param("statuses") List<Status> statuses);

    /**
     * Count patient appointments that overlap a given interval and are in one of the given statuses.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.patientUsername = :patientUsername
              AND a.startTime < :end
              AND a.endTime > :start
              AND a.status IN :statuses
            """)
    long countOverlappingForPatient(@Param("patientUsername") String patientUsername,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("statuses") List<Status> statuses);

    /**
     * Count patient appointments on a given day (by startTime) in one of the given statuses.
     */
    @Query("""
            SELECT COUNT(a) FROM Appointment a
            WHERE a.patientUsername = :patientUsername
              AND a.startTime >= :dayStart
              AND a.startTime < :dayEnd
              AND a.status IN :statuses
            """)
    long countForPatientOnDay(@Param("patientUsername") String patientUsername,
                              @Param("dayStart") LocalDateTime dayStart,
                              @Param("dayEnd") LocalDateTime dayEnd,
                              @Param("statuses") List<Status> statuses);
}
