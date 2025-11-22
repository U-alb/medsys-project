package org.wp2.medsys.appointmentsservice.dto;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(
        @NotBlank(message = "decision is required")
        String decision
) {}
