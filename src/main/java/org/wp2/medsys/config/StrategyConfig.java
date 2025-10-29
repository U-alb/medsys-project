package org.wp2.medsys.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.wp2.medsys.notify.EmailNotifier;
import org.wp2.medsys.notify.NoopNotifier;
import org.wp2.medsys.notify.Notifier;
import org.wp2.medsys.repositories.AppointmentRepository;
import org.wp2.medsys.strategy.BookingPolicy;
import org.wp2.medsys.strategy.BufferedOverbookPolicy;
import org.wp2.medsys.strategy.StrictPolicy;

@Slf4j
@Configuration
public class StrategyConfig {

    @Bean
    public BookingPolicy bookingPolicy(
            AppointmentRepository appointmentRepository,
            @Value("${medsys.booking:strict}") String policy,
            @Value("${medsys.booking.buffer:1}") int buffer
    ) {
        BookingPolicy bean = switch (policy.toLowerCase()) {
            case "buffered" -> new BufferedOverbookPolicy(appointmentRepository, buffer);
            default -> new StrictPolicy(appointmentRepository);
        };
        log.info("BookingPolicy active: {}", bean.name());
        return bean;
    }

    @Bean
    public Notifier notifier(@Value("${medsys.notify:none}") String notify) {
        Notifier bean = switch (notify.toLowerCase()) {
            case "email" -> new EmailNotifier();
            default -> new NoopNotifier();
        };
        log.info("Notifier active: {}", bean.getClass().getSimpleName());
        return bean;
    }
}
