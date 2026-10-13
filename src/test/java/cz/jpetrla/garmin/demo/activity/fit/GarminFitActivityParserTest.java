package cz.jpetrla.garmin.demo.activity.fit;

import com.garmin.fit.Sport;
import cz.jpetrla.garmin.demo.activity.ActivityData;
import cz.jpetrla.garmin.demo.activity.InvalidActivityFileException;
import cz.jpetrla.garmin.demo.testsupport.FitFileFixtures;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GarminFitActivityParserTest {

    private final GarminFitActivityParser parser = new GarminFitActivityParser();

    @Test
    void parse_extractsFieldsFromSingleSession() {
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1_000_000_001L, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertEquals("RUNNING", data.getSport());
        assertNotNull(data.getStartedAt());
    }

    @Test
    void parse_rejectsFileWithMultipleSessions() {
        byte[] fitBytes = FitFileFixtures.multiSessionFitFile(
                1_000_000_001L, Sport.CYCLING,
                1_000_005_000L, Sport.RUNNING);

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> parser.parse(new ByteArrayInputStream(fitBytes)));

        assertEquals("FIT files with multiple sessions are not supported.", exception.getMessage());
    }

    @Test
    void parse_rejectsChainedFileWithMultipleSessions() {
        byte[] fitBytes = FitFileFixtures.chainedFitFile(
                1_000_000_001L, Sport.CYCLING,
                1_000_005_000L, Sport.RUNNING);

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> parser.parse(new ByteArrayInputStream(fitBytes)));

        assertEquals("FIT files with multiple sessions are not supported.", exception.getMessage());
    }

    @Test
    void parse_rejectsCorruptFileWithSpecificException() {
        byte[] corruptFitBytes = "this is not a fit file".getBytes();

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> parser.parse(new ByteArrayInputStream(corruptFitBytes)));

        assertEquals("Invalid or corrupt FIT file.", exception.getMessage());
    }

    @Test
    void parse_preservesIOExceptionFromStreamRead() throws IOException {
        IOException readFailure = new IOException("Failed to read temporary upload file");
        try (InputStream brokenStream = new InputStream() {
            @Override
            public int available() {
                // The SDK checks availability before attempting to read a FIT segment.
                return 1;
            }

            @Override
            public int read() throws IOException {
                throw readFailure;
            }
        }) {

            UncheckedIOException exception = assertThrows(
                    UncheckedIOException.class,
                    () -> parser.parse(brokenStream));

            assertSame(readFailure, exception.getCause());
        }
    }

    @Test
    void parse_ignoresSystemTimeStartTimestamp() {
        // Raw values below DateTime.MIN (0x10000000) represent "seconds since device power-on",
        // not an absolute date, per the FIT protocol.
        byte[] fitBytes = FitFileFixtures.sessionFitFile(1000L, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertNull(data.getStartedAt());
    }

    @Test
    void parse_ignoresInvalidStartTimestamp() {
        // 0xFFFFFFFF is the FIT protocol's "field not present" sentinel for uint32 values.
        byte[] fitBytes = FitFileFixtures.sessionFitFile(0xFFFFFFFFL, Sport.RUNNING);

        ActivityData data = parser.parse(new ByteArrayInputStream(fitBytes));

        assertNull(data.getStartedAt());
    }
}
