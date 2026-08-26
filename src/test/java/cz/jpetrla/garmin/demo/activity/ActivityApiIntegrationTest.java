package cz.jpetrla.garmin.demo.activity;

import com.garmin.fit.Sport;
import cz.jpetrla.garmin.demo.testsupport.FitFileFixtures;
import cz.jpetrla.garmin.demo.testsupport.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class ActivityApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final Pattern LOCATION_ID_PATTERN = Pattern.compile("/api/activities/(\\d+)$");

    private void uploadSuccessfully(byte[] fitBytes, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/octet-stream", fitBytes);

        mockMvc.perform(multipart("/api/activities/upload").file(file))
                .andExpect(status().isCreated());
    }

    private static String extractId(String location) {
        Matcher matcher = LOCATION_ID_PATTERN.matcher(location);
        if (!matcher.find()) {
            throw new IllegalStateException("Unexpected Location header: " + location);
        }
        return matcher.group(1);
    }

    @Test
    void uploadGetListAndDeleteActivity_fullLifecycle() throws Exception {
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1_000_000_001L, Sport.RUNNING);
        MockMultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", fitBytes);

        String location = mockMvc.perform(multipart("/api/activities/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern("/api/activities/\\d+")))
                .andExpect(jsonPath("$.fitId").value(1_000_000_001L))
                .andExpect(jsonPath("$.sport").value("RUNNING"))
                .andExpect(jsonPath("$.heartRateAvg").value(140))
                .andReturn().getResponse().getHeader("Location");

        String id = extractId(location);

        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.fitId == 1000000001)]", hasSize(1)));

        mockMvc.perform(get("/api/activities/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(id)))
                .andExpect(jsonPath("$.fitId").value(1_000_000_001L));

        mockMvc.perform(delete("/api/activities/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/activities/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void upload_rejectsInvalidFitFile() throws Exception {
        MockMultipartFile garbage = new MockMultipartFile("file", "activity.fit", "application/octet-stream",
                "this is not a fit file".getBytes());

        mockMvc.perform(multipart("/api/activities/upload").file(garbage))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejectsMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/api/activities/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejectsEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "activity.fit", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/activities/upload").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejectsWrongFileExtension() throws Exception {
        MockMultipartFile wrongExtension = new MockMultipartFile("file", "activity.txt", "text/plain",
                "irrelevant".getBytes());

        mockMvc.perform(multipart("/api/activities/upload").file(wrongExtension))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_rejectsDuplicateFitId() throws Exception {
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1_000_000_002L, Sport.CYCLING);
        uploadSuccessfully(fitBytes, "activity.fit");

        MockMultipartFile duplicate = new MockMultipartFile("file", "activity-2.fit", "application/octet-stream", fitBytes);

        mockMvc.perform(multipart("/api/activities/upload").file(duplicate))
                .andExpect(status().isConflict());
    }

    @Test
    void getActivity_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/activities/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteActivity_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/api/activities/{id}", 999_999))
                .andExpect(status().isNotFound());
    }
}
