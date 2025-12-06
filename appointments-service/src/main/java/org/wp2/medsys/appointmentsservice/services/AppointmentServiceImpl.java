package org.wp2.medsys.appointmentsservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.domain.Status;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.errors.BookingConflictException;
import org.wp2.medsys.appointmentsservice.messaging.AppointmentEventPublisher;
import org.wp2.medsys.appointmentsservice.repositories.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository repo;
    private final AppointmentEventPublisher eventPublisher;

    @Value("${appointments.max-per-day-per-patient:3}")
    private int maxAppointmentsPerDayPerPatient;

    @Override
    @Transactional
    public Appointment create(AppointmentCreateDTO dto, String patientUsername) {
        validateTimes(dto.startTime(), dto.endTime());
        enforceBusinessRules(patientUsername, dto.doctorUsername(), dto.startTime(), dto.endTime());

        Appointment a = new Appointment();
        a.setPatientUsername(patientUsername);
        a.setDoctorUsername(dto.doctorUsername());
        a.setStartTime(dto.startTime());
        a.setEndTime(dto.endTime());
        a.setScheduleReason(dto.scheduleReason());
        a.setStatus(Status.PENDING);

        Appointment saved = repo.save(a);

        eventPublisher.publishCreated(saved);

        return saved;
    }

    @Override
    public List<Appointment> findAll() {
        return repo.findAll();
    }

    @Override
    public List<Appointment> findForPatient(String patientUsername) {
        return repo.findByPatientUsername(patientUsername);
    }

    @Override
    public List<Appointment> findForDoctor(String doctorUsername) {
        return repo.findByDoctorUsername(doctorUsername);
    }

    @Override
    @Transactional
    public Appointment decideStatus(Long id, String decision, String doctorUsername) {
        Appointment appointment = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));

        if (appointment.getStatus() != Status.PENDING) {
            throw new BookingConflictException("Only pending appointments can be changed.");
        }

        if (!appointment.getDoctorUsername().equals(doctorUsername)) {
            throw new AccessDeniedException("You can only decide your own appointments.");
        }

        Status newStatus = mapDecisionToStatus(decision);
        appointment.setStatus(newStatus);

        Appointment saved = repo.save(appointment);

        if (newStatus == Status.ACCEPTED) {
            eventPublisher.publishAccepted(saved);
        } else if (newStatus == Status.DENIED) {
            eventPublisher.publishRejected(saved);
        }

        return saved;
    }

    @Override
    @Transactional
    public Appointment cancel(Long id, String patientUsername) {
        Appointment appointment = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + id));

        if (!appointment.getPatientUsername().equals(patientUsername)) {
            throw new AccessDeniedException("You can only cancel your own appointments.");
        }

        if (appointment.getStatus() != Status.PENDING && appointment.getStatus() != Status.ACCEPTED) {
            throw new BookingConflictException("Only pending or accepted appointments can be cancelled.");
        }

        if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BookingConflictException("Cannot cancel past appointments.");
        }

        appointment.setStatus(Status.CANCELLED);

        Appointment saved = repo.save(appointment);

        eventPublisher.publishCancelled(saved);

        return saved;
    }

    /* ----------------- helpers ----------------- */

    private Status mapDecisionToStatus(String decision) {
        if (decision == null) {
            throw new IllegalArgumentException("Decision must be provided");
        }
        return switch (decision.toUpperCase()) {
            case "ACCEPT", "ACCEPTED" -> Status.ACCEPTED;
            case "DENY", "DENIED", "REJECT", "REJECTED" -> Status.DENIED;
            default -> throw new IllegalArgumentException("Unsupported decision: " + decision);
        };
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("startTime and endTime must be provided");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot schedule an appointment in the past");
        }
    }

    private void enforceBusinessRules(String patientUsername,
                                      String doctorUsername,
                                      LocalDateTime start,
                                      LocalDateTime end) {

        List<Status> relevantStatuses = List.of(Status.PENDING, Status.ACCEPTED);

        long doctorOverlaps = repo.countOverlappingForDoctor(
                doctorUsername,
                start,
                end,
                relevantStatuses
        );
        if (doctorOverlaps > 0) {
            throw new BookingConflictException("Selected doctor is not available in this time interval.");
        }

        long patientOverlaps = repo.countOverlappingForPatient(
                patientUsername,
                start,
                end,
                relevantStatuses
        );
        if (patientOverlaps > 0) {
            throw new BookingConflictException("You already have an appointment in this time interval.");
        }

        LocalDate day = start.toLocalDate();
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = LocalDateTime.of(day, LocalTime.MAX);

        long perDayCount = repo.countForPatientOnDay(
                patientUsername,
                dayStart,
                dayEnd,
                relevantStatuses
        );

        if (perDayCount >= maxAppointmentsPerDayPerPatient) {
            throw new BookingConflictException("You reached the daily limit of appointments.");
        }
    }
}
