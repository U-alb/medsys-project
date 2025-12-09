package org.wp2.medsys.appointmentsservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.wp2.medsys.appointmentsservice.domain.Appointment;
import org.wp2.medsys.appointmentsservice.domain.Status;
import org.wp2.medsys.appointmentsservice.dto.AppointmentCreateDTO;
import org.wp2.medsys.appointmentsservice.errors.BookingConflictException;
import org.wp2.medsys.appointmentsservice.messaging.AppointmentEventPublisher;
import org.wp2.medsys.appointmentsservice.repositories.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    AppointmentRepository repo;

    @Mock
    AppointmentEventPublisher eventPublisher;

    @InjectMocks
    AppointmentServiceImpl service;

    AppointmentCreateDTO dto;

    @BeforeEach
    void setup() {
        dto = new AppointmentCreateDTO(
                "doctor1",
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(30).withSecond(0).withNano(0),
                "Routine check"
        );
        // default maxAppointmentsPerDayPerPatient is injected via @Value; no need to set here
    }

    @Test
    void create_success_publishesCreatedEvent() {
        Appointment toSave = new Appointment();
        toSave.setPatientUsername("patient1");
        toSave.setDoctorUsername(dto.doctorUsername());
        toSave.setStartTime(dto.startTime());
        toSave.setEndTime(dto.endTime());
        toSave.setScheduleReason(dto.scheduleReason());
        toSave.setStatus(Status.PENDING);

        Appointment saved = new Appointment();
        saved.setId(5L);
        saved.setPatientUsername("patient1");
        saved.setDoctorUsername(dto.doctorUsername());
        saved.setStartTime(dto.startTime());
        saved.setEndTime(dto.endTime());
        saved.setScheduleReason(dto.scheduleReason());
        saved.setStatus(Status.PENDING);

        when(repo.countOverlappingForDoctor(anyString(), any(), any(), anyList())).thenReturn(0L);
        when(repo.countOverlappingForPatient(anyString(), any(), any(), anyList())).thenReturn(0L);
        when(repo.countForPatientOnDay(anyString(), any(), any(), anyList())).thenReturn(0L);
        when(repo.save(any(Appointment.class))).thenReturn(saved);

        Appointment result = service.create(dto, "patient1");

        assertNotNull(result);
        assertEquals(5L, result.getId());
        verify(repo).save(any());
        verify(eventPublisher).publishCreated(saved);
    }

    @Test
    void create_doctorOverlap_throwsBookingConflict() {
        when(repo.countOverlappingForDoctor(anyString(), any(), any(), anyList())).thenReturn(1L);

        BookingConflictException ex = assertThrows(BookingConflictException.class,
                () -> service.create(dto, "patient1"));
        assertTrue(ex.getMessage().toLowerCase().contains("doctor"));
        verify(repo, never()).save(any());
    }

    @Test
    void decideStatus_accept_changesToAccepted_andPublishes() {
        Appointment ap = new Appointment();
        ap.setId(10L);
        ap.setDoctorUsername("doctor1");
        ap.setPatientUsername("patient1");
        ap.setStatus(Status.PENDING);

        when(repo.findById(10L)).thenReturn(Optional.of(ap));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.decideStatus(10L, "ACCEPT", "doctor1");

        assertEquals(Status.ACCEPTED, result.getStatus());
        verify(repo).save(ap);
        verify(eventPublisher).publishAccepted(ap);
    }

    @Test
    void decideStatus_reject_changesToDenied_andPublishes() {
        Appointment ap = new Appointment();
        ap.setId(11L);
        ap.setDoctorUsername("doctor1");
        ap.setPatientUsername("patient1");
        ap.setStatus(Status.PENDING);

        when(repo.findById(11L)).thenReturn(Optional.of(ap));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment result = service.decideStatus(11L, "REJECT", "doctor1");

        assertEquals(Status.DENIED, result.getStatus());
        verify(eventPublisher).publishRejected(ap);
    }

    @Test
    void cancel_futureAccepted_setsCancelled_andPublishes() {
        Appointment ap = new Appointment();
        ap.setId(20L);
        ap.setPatientUsername("patient1");
        ap.setDoctorUsername("doctor1");
        ap.setStatus(Status.ACCEPTED);
        ap.setStartTime(LocalDateTime.now().plusDays(1));

        when(repo.findById(20L)).thenReturn(Optional.of(ap));
        when(repo.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment res = service.cancel(20L, "patient1");

        assertEquals(Status.CANCELLED, res.getStatus());
        verify(eventPublisher).publishCancelled(ap);
    }

    @Test
    void cancel_past_throwsBookingConflict() {
        Appointment ap = new Appointment();
        ap.setId(21L);
        ap.setPatientUsername("patient1");
        ap.setDoctorUsername("doctor1");
        ap.setStatus(Status.ACCEPTED);
        ap.setStartTime(LocalDateTime.now().minusDays(1));

        when(repo.findById(21L)).thenReturn(Optional.of(ap));

        BookingConflictException ex = assertThrows(BookingConflictException.class,
                () -> service.cancel(21L, "patient1"));
        assertTrue(ex.getMessage().toLowerCase().contains("past"));
        verify(eventPublisher, never()).publishCancelled(any());
    }

    @Test
    void enforceBusinessRules_dailyLimit_throwsWhenExceeded() {
        // simulate per-day count equal to max (default 3)
        LocalDate day = dto.startTime().toLocalDate();
        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = LocalDateTime.of(day, LocalTime.MAX);

        when(repo.countOverlappingForDoctor(anyString(), any(), any(), anyList())).thenReturn(0L);
        when(repo.countOverlappingForPatient(anyString(), any(), any(), anyList())).thenReturn(0L);
        when(repo.countForPatientOnDay(anyString(), eq(dayStart), eq(dayEnd), anyList())).thenReturn(3L);

        BookingConflictException ex = assertThrows(BookingConflictException.class,
                () -> service.create(dto, "patient1"));
        assertTrue(ex.getMessage().toLowerCase().contains("daily"));
    }
}
