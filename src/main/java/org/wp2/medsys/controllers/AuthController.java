package org.wp2.medsys.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.wp2.medsys.DTO.RegisterDTO;
import org.wp2.medsys.domain.*;
import org.wp2.medsys.repositories.UserRepository;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    /* ---------- views ---------- */

    @GetMapping("/login")
    public String login() {                     // just returns the login template
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        // empty DTO for the form-binding; (username, email, password, dateOfBirth, role)
        if (!model.containsAttribute("userForm")) {
            model.addAttribute("userForm", new RegisterDTO("", "", "", null, Role.PATIENT));
        }
        return "register";
    }

    @GetMapping("/portal")
    public String portalRedirect(Authentication authentication) {
        String username = authentication.getName(); // always works
        User user = repo.findByUsername(username).orElse(null);

        if (user == null) {
            return "redirect:/login?error=usernotfound";
        }

        return switch (user.getRole()) {
            case DOCTOR -> "redirect:/portal/doctorportal";
            case PATIENT -> "redirect:/portal/patientportal";
            default -> "redirect:/login?error=unknownrole";
        };
    }

    @GetMapping("/portal/doctorportal")
    public String doctorPortal() {
        return "portal/doctorportal";  // make sure templates/portal/doctorportal.html exists
    }

    @GetMapping("/portal/patientportal")
    public String patientPortal() {
        return "portal/patientportal"; // make sure templates/portal/patientportal.html exists
    }

    /* ---------- form POST ---------- */

    @PostMapping("/register")
    public String register(@ModelAttribute("userForm") RegisterDTO dto, RedirectAttributes ra) {
        // Default role if none provided
        Role role = (dto.role() != null) ? dto.role() : Role.PATIENT;

        // Basic duplicate guard on username (email dupes caught by DB/constraints)
        if (repo.findByUsername(dto.username()).isPresent()) {
            ra.addFlashAttribute("error", "Username is already taken.");
            ra.addFlashAttribute("userForm", dto);
            return "redirect:/register";
        }

        User user;
        switch (role) {
            case PATIENT -> {
                Patient p = new Patient();
                p.setUsername(dto.username());
                p.setEmail(dto.email());
                p.setPassHash(encoder.encode(dto.password()));
                p.setDateOfBirth(dto.dateOfBirth());
                p.setRole(Role.PATIENT);
                p.setCreatedAt(LocalDateTime.now());
                user = p;
            }
            case DOCTOR -> {
                Doctor d = new Doctor();
                d.setUsername(dto.username());
                d.setEmail(dto.email());
                d.setPassHash(encoder.encode(dto.password()));
                d.setDateOfBirth(dto.dateOfBirth());
                d.setRole(Role.DOCTOR);
                d.setCreatedAt(LocalDateTime.now());
                user = d;
            }
            // For M1 we don’t allow admin self-signup
            default -> {
                ra.addFlashAttribute("error", "Unsupported role selected.");
                ra.addFlashAttribute("userForm", dto);
                return "redirect:/register";
            }
        }

        try {
            repo.save(user);
        } catch (DataIntegrityViolationException e) {
            // Likely unique constraints (e.g., email)
            ra.addFlashAttribute("error", "An account with this email already exists.");
            ra.addFlashAttribute("userForm", dto);
            return "redirect:/register";
        }

        return "redirect:/login?registered";
    }
}
