package cz.jpetrla.garmin.demo.activity.infrastructure.fit;

import cz.jpetrla.garmin.demo.activity.domain.ActivityData;

import java.io.InputStream;

public interface FitActivityParser {

    ActivityData parse(InputStream inputStream);
}
