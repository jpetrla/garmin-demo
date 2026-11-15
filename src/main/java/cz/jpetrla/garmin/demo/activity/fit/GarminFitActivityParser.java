package cz.jpetrla.garmin.demo.activity.fit;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import cz.jpetrla.garmin.demo.activity.ActivityData;
import cz.jpetrla.garmin.demo.activity.InvalidActivityFileException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class GarminFitActivityParser implements FitActivityParser {

    @Override
    public ActivityData parse(InputStream inputStream) {
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
        List<SessionMesg> sessions = new ArrayList<>();
        broadcaster.addListener((SessionMesgListener) sessions::add);

        try {
            // This overload continues through chained FIT files instead of stopping after the
            // first segment, so every session reaches the validation below.
            decode.read(inputStream, broadcaster, broadcaster);
        } catch (FitRuntimeException e) {
            // The SDK also wraps stream I/O failures, which are not invalid user input.
            if (e.getCause() instanceof IOException ioException) {
                throw new UncheckedIOException("Failed to read uploaded file", ioException);
            }
            throw new InvalidActivityFileException("Invalid or corrupt FIT file.", e);
        }

        if (sessions.size() > 1) {
            throw new InvalidActivityFileException("FIT files with multiple sessions are not supported.");
        }

        // The start time is the one value every stored activity must have, so a file without it
        // is rejected here together with the other FIT validation rules.
        SessionMesg session = sessions.stream()
                .findFirst()
                .filter(s -> s.getStartTime() != null && isAbsoluteTimestamp(s.getStartTime()))
                .orElseThrow(() -> new InvalidActivityFileException(
                        "FIT file does not contain a session with a timestamp."));

        return toActivityData(session);
    }

    private ActivityData toActivityData(SessionMesg session) {
        ActivityData.ActivityDataBuilder builder = ActivityData.builder();
        builder.startedAt(session.getStartTime().getInstant());
        Optional.ofNullable(session.getSport()).map(Enum::name).ifPresent(builder::sport);
        builder.heartRateAvg(session.getAvgHeartRate());
        builder.trainingEffectAerobic(session.getTotalTrainingEffect());
        builder.trainingEffectAnaerobic(session.getTotalAnaerobicTrainingEffect());
        builder.totalDistance(session.getTotalDistance());
        builder.totalTimerTime(session.getTotalTimerTime());
        builder.totalMovingTime(session.getTotalMovingTime());
        builder.totalElapsedTime(session.getTotalElapsedTime());
        builder.maxHeartRate(session.getMaxHeartRate());
        builder.avgPower(session.getAvgPower());
        builder.maxPower(session.getMaxPower());
        builder.leftRightBalance(session.getLeftRightBalance());
        builder.avgLeftTorqueEffectiveness(session.getAvgLeftTorqueEffectiveness());
        builder.avgRightTorqueEffectiveness(session.getAvgRightTorqueEffectiveness());
        builder.avgLeftPedalSmoothness(session.getAvgLeftPedalSmoothness());
        builder.avgRightPedalSmoothness(session.getAvgRightPedalSmoothness());
        builder.normalizedPower(session.getNormalizedPower());
        builder.intensityFactor(session.getIntensityFactor());
        builder.trainingStressScore(session.getTrainingStressScore());
        return builder.build();
    }

    // FIT reserves values below DateTime.MIN for "seconds since device power-on" (not a real
    // date) and Fit.UINT32_INVALID for "field not present", so both must be excluded before a
    // raw timestamp can be treated as an absolute activity start time.
    private static boolean isAbsoluteTimestamp(DateTime dateTime) {
        long raw = dateTime.getTimestamp();
        return raw >= DateTime.MIN && raw != DateTime.INVALID;
    }
}
