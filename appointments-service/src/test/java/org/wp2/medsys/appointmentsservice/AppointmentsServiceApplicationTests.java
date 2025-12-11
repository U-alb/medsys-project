package org.wp2.medsys.appointmentsservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
// We need to import this because it is in a sub-package (.config)
import org.wp2.medsys.appointmentsservice.config.TestRabbitConfig;

@SpringBootTest
// Forces the test to use H2 DB and Dummy RabbitMQ instead
@Import({AppointmentServiceH2TestConfig.class, TestRabbitConfig.class})
class AppointmentsServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
