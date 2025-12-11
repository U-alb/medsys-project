package org.wp2.medsys.appointmentsservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.domain.Status;
import org.wp2.medsys.appointmentsservice.services.AppointmentService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentControllerTest {

    @Mock
    AppointmentService svc;

    @InjectMocks
    AppointmentController controller;

    MockMvc mvc;
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postAppointments_delegatesToService_andReturnsCreated() throws Exception {

        AppointmentCreateDTO dto = new AppointmentCreateDTO(
                "doctor1",
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 10, 30),
                "Routine check"
        );

        Appointment created = new Appointment();
        created.setId(1L);
        created.setPatientUsername("patient1");
        created.setDoctorUsername(dto.doctorUsername());
        created.setStartTime(dto.startTime());
        created.setEndTime(dto.endTime());
        created.setStatus(Status.PENDING);
        created.setScheduleReason(dto.scheduleReason());

        when(svc.create(any(), eq("patient1"))).thenReturn(created);

       mvc.perform(post("/appointments")
                .principal(new UsernamePasswordAuthenticationToken(
                        "patient1", 
                        null, 
                        java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PATIENT")) 
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(svc).create(any(), eq("patient1"));
    }
}
