package org.wp2.medsys.appointmentsservice.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.domain.Status;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.dto.AppointmentResponse;
import org.wp2.medsys.appointmentsservice.dto.DecisionRequest;
import org.wp2.medsys.appointmentsservice.services.AppointmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /* ---------- Helpers ---------- */

    private boolean hasRole(Authentication auth, String roleName) {
        if (auth == null) return false;
        String target = "ROLE_" + roleName;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (target.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getPatientUsername(),
                a.getDoctorUsername(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus(),
                a.getScheduleReason()
        );
    }

    /* ---------- Booking endpoints ---------- */

    /**
     * PATIENT books an appointment for themselves.
     * patientUsername is taken from JWT, not from body.
     */
    @PostMapping
    public ResponseEntity<?> create(Authentication authentication,
                                    @Valid @RequestBody AppointmentCreateDTO dto) {
        if (!hasRole(authentication, "PATIENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only patients can book appointments."
                    ));
        }

        String patientUsername = authentication.getName();
        Appointment created = appointmentService.create(dto, patientUsername);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    /**
     * Debug endpoint: all appointments (any authenticated user).
     */
    @GetMapping
    public List<AppointmentResponse> listAll() {
        return appointmentService.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ---------- Per-patient endpoints ---------- */

    /**
     * Current patient sees their own appointments, optionally filtered by status.
     * Example: GET /appointments/mine?status=PENDING
     */
    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication authentication,
                                  @RequestParam(required = false) String status) {
        if (!hasRole(authentication, "PATIENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only patients can view /appointments/mine."
                    ));
        }

        String username = authentication.getName();

        Status filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status filter: " + status);
            }
        }

        Status finalFilterStatus = filterStatus;
        List<AppointmentResponse> result = appointmentService.findForPatient(username)
                .stream()
                .filter(a -> finalFilterStatus == null || a.getStatus() == finalFilterStatus)
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Patient cancels their own upcoming appointment.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id,
                                    Authentication authentication) {
        if (!hasRole(authentication, "PATIENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only patients can cancel appointments."
                    ));
        }

        String patientUsername = authentication.getName();
        Appointment updated = appointmentService.cancel(id, patientUsername);
        return ResponseEntity.ok(toResponse(updated));
    }

    /* ---------- Per-doctor endpoints ---------- */

    /**
     * Current doctor sees their own appointments.
     * Optional filters: from/to (ISO date-time) + status.
     *
     * Example:
     * GET /appointments/doctor/me?from=2025-11-25T00:00:00&to=2025-11-26T00:00:00&status=PENDING
     */
    @GetMapping("/doctor/me")
    public ResponseEntity<?> doctorMine(
            Authentication authentication,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(required = false) String status) {

        if (!hasRole(authentication, "DOCTOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only doctors can view /appointments/doctor/me."
                    ));
        }

        String username = authentication.getName();

        Status filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid status filter: " + status);
            }
        }

        Status finalFilterStatus = filterStatus;
        List<AppointmentResponse> result = appointmentService.findForDoctor(username)
                .stream()
                .filter(a -> finalFilterStatus == null || a.getStatus() == finalFilterStatus)
                .filter(a -> from == null || !a.getStartTime().isBefore(from))
                .filter(a -> to == null || !a.getStartTime().isAfter(to))
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    /* ---------- Old-style debug endpoints ---------- */

    @GetMapping("/patient/{username}")
    public List<AppointmentResponse> forPatient(@PathVariable String username) {
        return appointmentService.findForPatient(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/doctor/{username}")
    public List<AppointmentResponse> forDoctor(@PathVariable String username) {
        return appointmentService.findForDoctor(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ---------- Decision endpoint (doctor) ---------- */

    @PostMapping("/{id}/decision")
    public ResponseEntity<?> decide(@PathVariable Long id,
                                    Authentication authentication,
                                    @Valid @RequestBody DecisionRequest request) {
        if (!hasRole(authentication, "DOCTOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only doctors can decide appointments."
                    ));
        }

        String doctorUsername = authentication.getName();
        Appointment updated = appointmentService.decideStatus(id, request.decision(), doctorUsername);
        return ResponseEntity.ok(toResponse(updated));
    }
}
