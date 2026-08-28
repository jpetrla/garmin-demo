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

    public static byte[] fitFileWithoutSession(long fitTimestamp) {
        return fitFile(new DateTime(fitTimestamp));
    }

    public static byte[] detailedSessionFitFile(long fitTimestamp, Sport sport) {
        SessionMesg session = session(fitTimestamp, sport);
        session.setTotalTrainingEffect(3.5f);
        session.setTotalAnaerobicTrainingEffect(1.3f);
        session.setTotalDistance(12_345.5f);
        session.setTotalTimerTime(3_600.5f);
        session.setTotalMovingTime(3_500.25f);
        session.setTotalElapsedTime(3_700.75f);
        session.setAvgHeartRate((short) 141);
        session.setMaxHeartRate((short) 189);
        session.setAvgPower(275);
        session.setMaxPower(910);
        session.setLeftRightBalance(52);
        session.setAvgLeftTorqueEffectiveness(72.5f);
        session.setAvgRightTorqueEffectiveness(73.5f);
        session.setAvgLeftPedalSmoothness(24.5f);
        session.setAvgRightPedalSmoothness(25.5f);
        session.setNormalizedPower(300);
        session.setIntensityFactor(0.875f);
        session.setTrainingStressScore(123.5f);
        return fitFile(session);
    }

    public static byte[] multiSessionFitFile(long firstTimestamp, Sport firstSport,
                                              long secondTimestamp, Sport secondSport) {
        return fitFile(session(firstTimestamp, firstSport), session(secondTimestamp, secondSport));
    }

    public static byte[] chainedFitFile(long firstTimestamp, Sport firstSport,
                                        long secondTimestamp, Sport secondSport) {
        byte[] first = sessionFitFile(firstTimestamp, firstSport);
        byte[] second = sessionFitFile(secondTimestamp, secondSport);

        byte[] chained = new byte[first.length + second.length];
        System.arraycopy(first, 0, chained, 0, first.length);
        System.arraycopy(second, 0, chained, first.length, second.length);
        return chained;
    }

    private static byte[] fitFile(SessionMesg... sessions) {
        return fitFile(new DateTime(sessions[0].getStartTime()), sessions);
    }

    private static byte[] fitFile(DateTime createdAt, SessionMesg... sessions) {
        try {
            Path tempFile = Files.createTempFile("activity", ".fit");
            try {
                FileEncoder encoder = new FileEncoder(tempFile.toFile(), Fit.ProtocolVersion.V2_0);

                FileIdMesg fileId = new FileIdMesg();
                fileId.setType(File.ACTIVITY);
                fileId.setManufacturer(1);
                fileId.setProduct(1);
                fileId.setSerialNumber(1L);
                fileId.setTimeCreated(createdAt);
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
