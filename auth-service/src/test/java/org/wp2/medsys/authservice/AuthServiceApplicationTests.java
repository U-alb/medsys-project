package org.wp2.medsys.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(AuthServiceH2TestConfig.class)
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
