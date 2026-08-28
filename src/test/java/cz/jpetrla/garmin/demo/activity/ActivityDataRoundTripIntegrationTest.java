package cz.jpetrla.garmin.demo.activity;

import com.garmin.fit.DateTime;
import com.garmin.fit.Sport;
import cz.jpetrla.garmin.demo.testsupport.FitFileFixtures;
import cz.jpetrla.garmin.demo.testsupport.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Sql(
        statements = "TRUNCATE TABLE activity RESTART IDENTITY",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ActivityDataRoundTripIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void upload_roundTripsAllSupportedSessionFields() throws Exception {
        long fitTimestamp = 1_000_000_010L;
        byte[] fitBytes = FitFileFixtures.detailedSessionFitFile(fitTimestamp, Sport.CYCLING);
        MockMultipartFile file = new MockMultipartFile(
                "file", "detailed.fit", "application/octet-stream", fitBytes);

        String location = mockMvc.perform(multipart("/api/activities/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern("/api/activities/\\d+")))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sport").value("CYCLING"))
                .andExpect(jsonPath("$.heartRateAvg").value(141))
                .andExpect(jsonPath("$.trainingEffectAerobic").value(3.5))
                .andExpect(jsonPath("$.trainingEffectAnaerobic").value(1.3))
                .andExpect(jsonPath("$.startedAt").value(new DateTime(fitTimestamp).getInstant().toString()))
                .andExpect(jsonPath("$.totalDistance").value(12_345.5))
                .andExpect(jsonPath("$.totalTimerTime").value(3_600.5))
                .andExpect(jsonPath("$.totalMovingTime").value(3_500.25))
                .andExpect(jsonPath("$.totalElapsedTime").value(3_700.75))
                .andExpect(jsonPath("$.maxHeartRate").value(189))
                .andExpect(jsonPath("$.avgPower").value(275))
                .andExpect(jsonPath("$.maxPower").value(910))
                .andExpect(jsonPath("$.leftRightBalance").value(52))
                .andExpect(jsonPath("$.avgLeftTorqueEffectiveness").value(72.5))
                .andExpect(jsonPath("$.avgRightTorqueEffectiveness").value(73.5))
                .andExpect(jsonPath("$.avgLeftPedalSmoothness").value(24.5))
                .andExpect(jsonPath("$.avgRightPedalSmoothness").value(25.5))
                .andExpect(jsonPath("$.normalizedPower").value(300))
                .andExpect(jsonPath("$.intensityFactor").value(0.875))
                .andExpect(jsonPath("$.trainingStressScore").value(123.5));
    }
}
