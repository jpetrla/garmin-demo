package cz.jpetrla.garmin.demo.activity.exception;

import cz.jpetrla.garmin.demo.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ActivityNotFoundException extends ApiException {

    public ActivityNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Activity with id " + id + " not found.");
    }
}
