package org.wp2.medsys.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.notify.Notifier;
import org.wp2.medsys.repositories.AppointmentRepository;
import org.wp2.medsys.strategy.BookingPolicy;

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
    public Appointment create(Appointment appointment) {
        // Basic sanity checks
        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new IllegalArgumentException("Doctor must be specified.");
        }
        LocalDateTime when = appointment.getAppointmentDate();
        if (when == null) {
            throw new IllegalArgumentException("Appointment date/time must be specified.");
        }

        // Strategy: booking policy
        bookingPolicy.assertCanBook(appointment.getDoctor().getId(), when);

        // Persist
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment created: id={}, doctorId={}, patientId={}, when={}",
                saved.getId(),
                saved.getDoctor() != null ? saved.getDoctor().getId() : null,
                saved.getPatient() != null ? saved.getPatient().getId() : null,
                saved.getAppointmentDate());

        // Strategy: notifier (stub)
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
}
