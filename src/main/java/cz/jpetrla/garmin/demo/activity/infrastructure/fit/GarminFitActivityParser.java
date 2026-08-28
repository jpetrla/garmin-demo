package cz.jpetrla.garmin.demo.activity.infrastructure.fit;

import com.garmin.fit.DateTime;
import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import cz.jpetrla.garmin.demo.activity.application.FitActivityParser;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.exception.InvalidActivityFileException;
import org.springframework.stereotype.Component;

import java.io.InputStream;
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
            throw new InvalidActivityFileException("Invalid or corrupt FIT file.", e);
        }

        if (sessions.size() > 1) {
            throw new InvalidActivityFileException("FIT files with multiple sessions are not supported.");
        }

        ActivityData.ActivityDataBuilder builder = ActivityData.builder();
        if (!sessions.isEmpty()) {
            populateBuilder(builder, sessions.getFirst());
        }
        return builder.build();
    }

    private void populateBuilder(ActivityData.ActivityDataBuilder builder, SessionMesg session) {
        Optional.ofNullable(session.getStartTime())
                .filter(GarminFitActivityParser::isAbsoluteTimestamp)
                .ifPresent(dt -> builder.startedAt(dt.getInstant()));
        Optional.ofNullable(session.getSport()).map(Enum::name).ifPresent(builder::sport);
        Optional.ofNullable(session.getAvgHeartRate()).ifPresent(builder::heartRateAvg);
        Optional.ofNullable(session.getTotalTrainingEffect()).ifPresent(builder::trainingEffectAerobic);
        Optional.ofNullable(session.getTotalAnaerobicTrainingEffect()).ifPresent(builder::trainingEffectAnaerobic);
        Optional.ofNullable(session.getTotalDistance()).ifPresent(builder::totalDistance);
        Optional.ofNullable(session.getTotalTimerTime()).ifPresent(builder::totalTimerTime);
        Optional.ofNullable(session.getTotalMovingTime()).ifPresent(builder::totalMovingTime);
        Optional.ofNullable(session.getTotalElapsedTime()).ifPresent(builder::totalElapsedTime);
        Optional.ofNullable(session.getMaxHeartRate()).ifPresent(builder::maxHeartRate);
        Optional.ofNullable(session.getAvgPower()).ifPresent(builder::avgPower);
        Optional.ofNullable(session.getMaxPower()).ifPresent(builder::maxPower);
        Optional.ofNullable(session.getLeftRightBalance()).ifPresent(builder::leftRightBalance);
        Optional.ofNullable(session.getAvgLeftTorqueEffectiveness()).ifPresent(builder::avgLeftTorqueEffectiveness);
        Optional.ofNullable(session.getAvgRightTorqueEffectiveness()).ifPresent(builder::avgRightTorqueEffectiveness);
        Optional.ofNullable(session.getAvgLeftPedalSmoothness()).ifPresent(builder::avgLeftPedalSmoothness);
        Optional.ofNullable(session.getAvgRightPedalSmoothness()).ifPresent(builder::avgRightPedalSmoothness);
        Optional.ofNullable(session.getNormalizedPower()).ifPresent(builder::normalizedPower);
        Optional.ofNullable(session.getIntensityFactor()).ifPresent(builder::intensityFactor);
        Optional.ofNullable(session.getTrainingStressScore()).ifPresent(builder::trainingStressScore);
    }

    // FIT reserves values below DateTime.MIN for "seconds since device power-on" (not a real
    // date) and Fit.UINT32_INVALID for "field not present", so both must be excluded before a
    // raw timestamp can be treated as an absolute activity start time.
    private static boolean isAbsoluteTimestamp(DateTime dateTime) {
        long raw = dateTime.getTimestamp();
        return raw >= DateTime.MIN && raw != DateTime.INVALID;
    }
}
