package org.wp2.medsys.appointmentsservice.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.dto.AppointmentResponse;
import org.wp2.medsys.appointmentsservice.dto.DecisionRequest;
import org.wp2.medsys.appointmentsservice.errors.BookingConflictException;
import org.wp2.medsys.appointmentsservice.services.AppointmentService;

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
     * Main booking endpoint: PATIENT books an appointment for themselves.
     * - patientUsername is taken from JWT, not from request body.
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

        try {
            Appointment created = appointmentService.create(dto, patientUsername);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(toResponse(created));
        } catch (BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", 409,
                            "error", "Conflict",
                            "message", ex.getMessage()
                    ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "error", "Bad Request",
                            "message", ex.getMessage()
                    ));
        }
    }

    /**
     * Debug endpoint: list all appointments (any authenticated user).
     */
    @GetMapping
    public List<AppointmentResponse> listAll() {
        return appointmentService.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ---------- Per-user endpoints (JWT-based) ---------- */

    /**
     * Patient view: current patient sees their own appointments.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> mine(Authentication authentication) {
        if (!hasRole(authentication, "PATIENT")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only patients can view /appointments/mine."
                    ));
        }

        String username = authentication.getName();

        List<AppointmentResponse> result = appointmentService.findForPatient(username)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Doctor view: current doctor sees their own appointments.
     */
    @GetMapping("/doctor/me")
    public ResponseEntity<?> doctorMine(Authentication authentication) {
        if (!hasRole(authentication, "DOCTOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Only doctors can view /appointments/doctor/me."
                    ));
        }

        String username = authentication.getName();

        List<AppointmentResponse> result = appointmentService.findForDoctor(username)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    /* ---------- Old-style per-username endpoints (debug) ---------- */

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

    /* ---------- Decision endpoint ---------- */

    /**
     * Doctor decides pending appointment (ACCEPT / DENY) for their own appointments.
     */
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

        try {
            Appointment updated = appointmentService.decideStatus(id, request.decision(), doctorUsername);
            return ResponseEntity.ok(toResponse(updated));
        } catch (BookingConflictException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "status", 409,
                            "error", "Conflict",
                            "message", ex.getMessage()
                    ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "error", "Bad Request",
                            "message", ex.getMessage()
                    ));
        }
    }
}
