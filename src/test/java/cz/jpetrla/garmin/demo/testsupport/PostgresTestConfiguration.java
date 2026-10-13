package cz.jpetrla.garmin.demo.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfiguration {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Spring Boot manages the container bean lifecycle.
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("garmin_demo")
                .withUsername("garmin")
                .withPassword("garmin");
    }
}
