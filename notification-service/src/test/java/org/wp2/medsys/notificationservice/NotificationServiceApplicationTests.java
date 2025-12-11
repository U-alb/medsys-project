package org.wp2.medsys.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(NotificationServiceH2TestConfig.class)       // Loading H2 DB and mock Rabbit  
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
