package cz.jpetrla.garmin.demo.activity.application;

import cz.jpetrla.garmin.demo.activity.domain.ActivityData;

import java.io.InputStream;

public interface FitActivityParser {

    ActivityData parse(InputStream inputStream);
}
