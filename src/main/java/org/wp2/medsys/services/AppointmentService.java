package org.wp2.medsys.services;

import org.wp2.medsys.domain.Appointment;

import java.util.List;
import java.util.Optional;

public interface AppointmentService {
    List<Appointment> findAll();
    Optional<Appointment> findById(Long id);
    Appointment create(Appointment appointment) throws Throwable;
    Appointment update(Appointment appointment);
    void deleteById(Long id);

    void deleteAll();
}
