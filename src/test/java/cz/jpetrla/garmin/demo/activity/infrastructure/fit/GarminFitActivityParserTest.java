package cz.jpetrla.garmin.demo.activity.infrastructure.fit;

import com.garmin.fit.Sport;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.testsupport.FitFileFixtures;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class GarminFitActivityParserTest {

    private final GarminFitActivityParser parser = new GarminFitActivityParser();

    @Test
    void parse_extractsFieldsFromSingleSession() {
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1_000_000_001L, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertThat(data.getFitId()).isEqualTo(1_000_000_001L);
        assertThat(data.getSport()).isEqualTo("RUNNING");
        assertThat(data.getStartedAt()).isNotNull();
    }

    @Test
    void parse_usesOnlyFirstSessionWhenFileContainsMultipleSessions() {
        byte[] fitBytes = FitFileFixtures.multiSessionFitFile(
                1_000_000_001L, Sport.CYCLING,
                1_000_005_000L, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertThat(data.getFitId()).isEqualTo(1_000_000_001L);
        assertThat(data.getSport()).isEqualTo("CYCLING");
    }

    @Test
    void parse_ignoresSystemTimeStartTimestamp() {
        // Raw values below DateTime.MIN (0x10000000) represent "seconds since device power-on",
        // not an absolute date, per the FIT protocol.
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1000L, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertThat(data.getFitId()).isNull();
        assertThat(data.getStartedAt()).isNull();
    }

    @Test
    void parse_ignoresInvalidStartTimestamp() {
        // 0xFFFFFFFF is the FIT protocol's "field not present" sentinel for uint32 values.
        byte[] fitBytes = FitFileFixtures.sessionFitFile(0xFFFFFFFFL, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertThat(data.getFitId()).isNull();
        assertThat(data.getStartedAt()).isNull();
    }
}
