package org.wp2.medsys.notificationservice.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugAuthController {

    @GetMapping("/whoami")
    public Map<String, Object> whoAmI(Authentication authentication) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (authentication == null) {
            result.put("authenticated", false);
            result.put("principal", null);      // LinkedHashMap allows null
            result.put("authorities", List.of());
            result.put("detailsClass", null);
            return result;
        }

        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        result.put("authenticated", authentication.isAuthenticated());
        result.put("principal", authentication.getName());
        result.put("authorities", authorities);
        result.put("detailsClass", authentication.getClass().getName());

        return result;
    }
}
