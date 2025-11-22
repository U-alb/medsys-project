package org.wp2.medsys.appointmentsservice.services;

import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;

import java.util.List;

public interface AppointmentService {

    Appointment create(AppointmentCreateDTO dto, String patientUsername);

    List<Appointment> findAll();

    List<Appointment> findForPatient(String patientUsername);

    List<Appointment> findForDoctor(String doctorUsername);

    Appointment decideStatus(Long id, String decision, String doctorUsername);

    Appointment cancel(Long id, String patientUsername);
}
