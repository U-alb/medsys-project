package org.wp2.medsys.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.wp2.medsys.domain.Appointment;
import org.wp2.medsys.services.AppointmentService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AppointmentDecisionController {

    private final AppointmentService appointmentService;

    /**
     * Doctor decides an appointment from the dashboard.
     * Expects a form field "decision" with value "ACCEPT" or "DENY".
     */
    @PostMapping("/appointments/{id}/decision")
    @PreAuthorize("hasRole('DOCTOR')")
    public String decide(@PathVariable("id") Long id,
                         @RequestParam("decision") String decision,
                         RedirectAttributes ra) throws Throwable {
        Appointment updated = appointmentService.decide(id, decision);
        ra.addFlashAttribute("decided", true);
        ra.addFlashAttribute("decidedId", updated.getId());
        ra.addFlashAttribute("decidedStatus", updated.getStatus().name());
        return "redirect:/doctor/dashboard";
    }
}
