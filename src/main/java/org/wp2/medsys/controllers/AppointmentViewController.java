package org.wp2.medsys.controllers;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.domain.*;
import org.wp2.medsys.services.*;
import org.wp2.medsys.repositories.UserRepository;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/appointments")
public class AppointmentViewController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final UserRepository userRepository;

    public AppointmentViewController(
            AppointmentService appointmentService,
            DoctorService doctorService,
            UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.doctorService = doctorService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String showAppointmentForm(Model model) {
        model.addAttribute("doctors", doctorService.findAll());
        return "appointments/appointmentform";
    }

    @PostMapping("/schedule")
    public String scheduleAppointment(
            @RequestParam LocalDateTime appointmentDate,
            @RequestParam Long doctorId,
            @RequestParam String scheduleReason,
            Authentication authentication) throws Throwable {
        
        // Get the current logged-in patient
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found"));
        
        if (!(user instanceof Patient)) {
            throw new IllegalStateException("Only patients can schedule appointments");
        }
        
        Patient patient = (Patient) user;
        Doctor doctor = doctorService.findById(doctorId);
        
        // Create and save the appointment
        Appointment appointment = new Appointment(
                appointmentDate,
                patient,
                doctor,
                scheduleReason,
                Status.PENDING
        );
        
        appointmentService.create(appointment);
        
        return "redirect:/portal/patientportal?scheduled=true";
    }
} 