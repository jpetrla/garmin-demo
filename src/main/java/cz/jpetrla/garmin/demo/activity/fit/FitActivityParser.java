package cz.jpetrla.garmin.demo.activity.fit;

import cz.jpetrla.garmin.demo.activity.ActivityData;

import java.io.InputStream;

public interface FitActivityParser {

    ActivityData parse(InputStream inputStream);
}
