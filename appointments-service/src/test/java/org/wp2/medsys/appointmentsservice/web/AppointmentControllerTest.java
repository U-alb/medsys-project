package org.wp2.medsys.appointmentsservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.services.AppointmentService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentControllerTest {

    @Mock
    AppointmentService svc;

    @InjectMocks
    AppointmentController controller;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void postAppointments_delegatesToService_andReturnsCreated() throws Exception {
        MockitoAnnotations.openMocks(this);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        AppointmentCreateDTO dto = new AppointmentCreateDTO(
                "doctor1",
                LocalDateTime.of(2025,12,1,10,0),
                LocalDateTime.of(2025,12,1,10,30),
                "Routine check"
        );

        // controller extracts patient username from security — for unit test we can stub service to avoid nulls
        when(svc.create(any(), any())).thenReturn(null);

        mvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(svc).create(any(), any());
    }
}
