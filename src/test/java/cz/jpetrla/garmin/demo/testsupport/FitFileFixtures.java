package cz.jpetrla.garmin.demo.testsupport;

import com.garmin.fit.DateTime;
import com.garmin.fit.File;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds minimal, valid FIT files in-memory (via the Garmin FIT SDK encoder) so tests
 * don't depend on binary fixture files checked into the repo.
 */
public final class FitFileFixtures {

    private FitFileFixtures() {
    }

    public static byte[] sessionFitFile(long fitTimestamp, Sport sport) {
        return fitFile(session(fitTimestamp, sport));
    }

    public static byte[] multiSessionFitFile(long firstTimestamp, Sport firstSport,
                                              long secondTimestamp, Sport secondSport) {
        return fitFile(session(firstTimestamp, firstSport), session(secondTimestamp, secondSport));
    }

    private static byte[] fitFile(SessionMesg... sessions) {
        try {
            Path tempFile = Files.createTempFile("activity", ".fit");
            try {
                FileEncoder encoder = new FileEncoder(tempFile.toFile(), Fit.ProtocolVersion.V2_0);

                FileIdMesg fileId = new FileIdMesg();
                fileId.setType(File.ACTIVITY);
                fileId.setManufacturer(1);
                fileId.setProduct(1);
                fileId.setSerialNumber(1L);
                fileId.setTimeCreated(new DateTime(sessions[0].getStartTime()));
                encoder.write(fileId);

                for (SessionMesg session : sessions) {
                    encoder.write(session);
                }

                encoder.close();

                return Files.readAllBytes(tempFile);
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SessionMesg session(long fitTimestamp, Sport sport) {
        SessionMesg session = new SessionMesg();
        session.setStartTime(new DateTime(fitTimestamp));
        session.setSport(sport);
        session.setAvgHeartRate((short) 140);
        session.setMaxHeartRate((short) 175);
        session.setTotalDistance(5000f);
        session.setTotalTimerTime(1800f);
        return session;
    }
}
