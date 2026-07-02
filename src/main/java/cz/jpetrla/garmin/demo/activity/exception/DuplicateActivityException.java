package cz.jpetrla.garmin.demo.activity.exception;

import cz.jpetrla.garmin.demo.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateActivityException extends ApiException {

    public DuplicateActivityException(Long fitId) {
        super(HttpStatus.CONFLICT, "Activity with fitId " + fitId + " already exists.");
    }
}
