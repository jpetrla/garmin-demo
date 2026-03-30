package cz.jpetrla.garmin.demo.activity.api;

public record ActivityUploadResponse (
        String sport,
        Short heartRateAvg,
        Float trainingEffectAerobic,
        Float trainingEffectAnaerobic
) {
}
