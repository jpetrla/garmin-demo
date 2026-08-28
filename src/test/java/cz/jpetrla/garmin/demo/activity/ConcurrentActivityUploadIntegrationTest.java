package cz.jpetrla.garmin.demo.activity;

import com.garmin.fit.Sport;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityRepository;
import cz.jpetrla.garmin.demo.testsupport.FitFileFixtures;
import cz.jpetrla.garmin.demo.testsupport.PostgresTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@Sql(
        statements = "TRUNCATE TABLE activity RESTART IDENTITY",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class ConcurrentActivityUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private ActivityRepository activityRepository;

    @Test
    void simultaneousDuplicateUploads_returnOneCreatedAndOneConflict() throws Exception {
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1_000_000_020L, Sport.RUNNING);
        CyclicBarrier bothRequestsPassedPrecheck = new CyclicBarrier(2);
        doAnswer(invocation -> {
            bothRequestsPassedPrecheck.await(10, TimeUnit.SECONDS);
            return false;
        }).when(activityRepository).existsByFileHash(anyString());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> uploadStatus(fitBytes, "activity-1.fit"));
            Future<Integer> second = executor.submit(() -> uploadStatus(fitBytes, "activity-2.fit"));

            List<Integer> statuses = List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS));

            assertEquals(List.of(201, 409), statuses.stream().sorted().toList());
            assertEquals(1, activityRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    private int uploadStatus(byte[] fitBytes, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/octet-stream", fitBytes);
        return mockMvc.perform(multipart("/api/activities/upload").file(file))
                .andReturn().getResponse().getStatus();
    }
}
