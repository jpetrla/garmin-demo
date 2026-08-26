package cz.jpetrla.garmin.demo;

import cz.jpetrla.garmin.demo.testsupport.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
class GarminDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
