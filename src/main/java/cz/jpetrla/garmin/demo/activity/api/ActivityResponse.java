package cz.jpetrla.garmin.demo.activity.api;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        String sport,
        Short heartRateAvg,
        Float trainingEffectAerobic,
        Float trainingEffectAnaerobic,
        Instant startedAt,
        Float totalDistance,
        Float totalTimerTime,
        Float totalMovingTime,
        Float totalElapsedTime,
        Short maxHeartRate,
        Integer avgPower,
        Integer maxPower,
        Integer leftRightBalance,
        Float avgLeftTorqueEffectiveness,
        Float avgRightTorqueEffectiveness,
        Float avgLeftPedalSmoothness,
        Float avgRightPedalSmoothness,
        Integer normalizedPower,
        Float intensityFactor,
        Float trainingStressScore
) {
}
