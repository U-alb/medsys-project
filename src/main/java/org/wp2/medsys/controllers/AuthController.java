package org.wp2.medsys.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.wp2.medsys.DTO.RegisterDTO;
import org.wp2.medsys.domain.Role;
import org.wp2.medsys.domain.User;
import org.wp2.medsys.repositories.UserRepository;
import org.wp2.medsys.services.RegistrationService;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository repo;                // used for portal redirect & username pre-check
    private final RegistrationService registrationService;

    /* ---------- views ---------- */

    @GetMapping("/login")
    public String login() {                           // just returns the login template
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        // empty DTO for the form-binding: (username, email, password, dateOfBirth, role)
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
        return "portal/doctorportal";  // ensure templates/portal/doctorportal.html exists
    }

    @GetMapping("/portal/patientportal")
    public String patientPortal() {
        return "portal/patientportal"; // ensure templates/portal/patientportal.html exists
    }

    /* ---------- form POST ---------- */

    @PostMapping("/register")
    public String register(@ModelAttribute("userForm") RegisterDTO dto, RedirectAttributes ra) {
        // Friendly duplicate guard on username (email/other uniques handled by DB)
        if (repo.findByUsername(dto.username()).isPresent()) {
            ra.addFlashAttribute("error", "Username is already taken.");
            ra.addFlashAttribute("userForm", dto);
            return "redirect:/register";
        }

        try {
            registrationService.register(dto);  // delegates to factory + password encoder + save
        } catch (IllegalArgumentException e) {
            // e.g., unsupported role (shouldn't happen with current DTO defaults)
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("userForm", dto);
            return "redirect:/register";
        } catch (DataIntegrityViolationException e) {
            // Likely unique constraints (email, license_number, etc.)
            ra.addFlashAttribute("error", "Account already exists with provided data (email or other unique field).");
            ra.addFlashAttribute("userForm", dto);
            return "redirect:/register";
        }

        return "redirect:/login?registered";
    }
}
