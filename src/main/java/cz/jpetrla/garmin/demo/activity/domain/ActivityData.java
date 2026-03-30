package cz.jpetrla.garmin.demo.activity.domain;

import lombok.Builder;

@Builder
public record ActivityData(
   String sport,
   Short heartRateAvg,
   Float trainingEffectAerobic,
   Float trainingEffectAnaerobic
) {
}
