package org.wp2.medsys.appointmentsservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@TestConfiguration
public class AppointmentServiceH2TestConfig {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:apptdb;DB_CLOSE_DELAY=-1;MODE=MYSQL");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    //Required for AppointmentServiceImplTest
    @Bean
    public Integer maxAppointmentsPerDayPerPatient() {
        return 999;  // override business rule for unit tests
    }
}
