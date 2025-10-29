package org.wp2.medsys.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.domain.Status;
import org.wp2.medsys.errors.UnauthorizedException;
import org.wp2.medsys.notify.Notifier;
import org.wp2.medsys.repositories.AppointmentRepository;
import org.wp2.medsys.strategy.BookingPolicy;
import org.wp2.medsys.validation.ValidationHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BookingPolicy bookingPolicy;
    private final Notifier notifier;
    private final ValidationHandler bookingValidationChain;

    @Override
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    @Override
    @Transactional
    public Appointment create(Appointment appointment) throws Throwable {
        // ---- Chain of Responsibility: pre-validations ----
        bookingValidationChain.handle(appointment);

        // ---- Strategy: booking policy (doctor capacity/overlaps) ----
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Doctor must be specified.");
        }
        LocalDateTime when = appointment.getAppointmentDate();
        if (when == null) {
            throw new IllegalArgumentException("Appointment date/time must be specified.");
        }
        bookingPolicy.assertCanBook(appointment.getDoctor().getId(), when);

        // ---- Persist + Notify ----
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment created: id={}, doctorId={}, patientId={}, when={}, policy={}",
                saved.getId(),
                saved.getDoctor() != null ? saved.getDoctor().getId() : null,
                saved.getPatient() != null ? saved.getPatient().getId() : null,
                saved.getAppointmentDate(),
                bookingPolicy.name());

        notifier.onAppointmentScheduled(saved);
        return saved;
    }

    @Override
    public Appointment update(Appointment appointment) {
        if (appointment.getId() == null || !appointmentRepository.existsById(appointment.getId())) {
            throw new IllegalArgumentException("Appointment not found for update.");
        }
        return appointmentRepository.save(appointment);
    }

    @Override
    public void deleteById(Long id) {
        appointmentRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        appointmentRepository.deleteAll();
    }

    @Override
    @Transactional
    public Appointment decide(Long appointmentId, String decision) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("You must be logged in.");
        }
        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DOCTOR".equals(a.getAuthority()));
        if (!isDoctor) {
            throw new Throwable("Only doctors can decide appointments.");
        }

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow();

        String loggedUser = auth.getName();
        if (appt.getDoctor() == null || appt.getDoctor().getUsername() == null
                || !appt.getDoctor().getUsername().equals(loggedUser)) {
            throw new Throwable("You can only decide your own appointments.");
        }

        if (appt.getStatus() != Status.PENDING) {
            throw new Throwable("Only PENDING appointments can be decided.");
        }

        Status newStatus = mapDecision(decision);
        appt.setStatus(newStatus);

        Appointment saved = appointmentRepository.save(appt);
        log.info("Appointment decided: id={}, doctorId={}, decision={}",
                saved.getId(),
                saved.getDoctor() != null ? saved.getDoctor().getId() : null,
                newStatus);

        notifier.onAppointmentDecided(saved);
        return saved;
    }

    private Status mapDecision(String decision) throws Throwable {
        if (decision == null) throw new Throwable("Decision must be provided.");
        String d = decision.trim().toUpperCase();
        // Accept common forms
        if ("ACCEPT".equals(d) || "ACCEPTED".equals(d)) return Status.ACCEPTED;
        if ("DENY".equals(d) || "DENIED".equals(d)) return Status.DENIED;
        throw new Throwable("Decision must be ACCEPT or DENY.");
    }
}
