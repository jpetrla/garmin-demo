package cz.jpetrla.garmin.demo.activity.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ActivityData {
    String sport;
    Short heartRateAvg;
    Float trainingEffectAerobic;
    Float trainingEffectAnaerobic;
    Instant startedAt;
    Float totalDistance;
    Float totalTimerTime;
    Float totalMovingTime;
    Float totalElapsedTime;
    Short maxHeartRate;
    Integer avgPower;
    Integer maxPower;
    Integer leftRightBalance;
    Float avgLeftTorqueEffectiveness;
    Float avgRightTorqueEffectiveness;
    Float avgLeftPedalSmoothness;
    Float avgRightPedalSmoothness;
    Integer normalizedPower;
    Float intensityFactor;
    Float trainingStressScore;
}
