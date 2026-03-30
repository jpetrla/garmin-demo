package cz.jpetrla.garmin.demo.activity.infrastructure.fit;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

@Component
public class GarminFitActivityParser implements FitActivityParser {

    @Override
    public ActivityData parse(InputStream inputStream) {
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

        ActivityData.ActivityDataBuilder activityDataBuilder = ActivityData.builder();
        broadcaster.addListener((SessionMesgListener) session -> populateBuilder(activityDataBuilder, session));
        decode.read(inputStream, broadcaster);

        return activityDataBuilder.build();
    }

    private void populateBuilder(ActivityData.ActivityDataBuilder builder, SessionMesg session) {
        Optional.ofNullable(session.getSport()).map(Enum::name).ifPresent(builder::sport);
        Optional.ofNullable(session.getAvgHeartRate()).ifPresent(builder::heartRateAvg);
        Optional.ofNullable(session.getTotalTrainingEffect()).ifPresent(builder::trainingEffectAerobic);
        Optional.ofNullable(session.getTotalAnaerobicTrainingEffect()).ifPresent(builder::trainingEffectAnaerobic);
    }
}
