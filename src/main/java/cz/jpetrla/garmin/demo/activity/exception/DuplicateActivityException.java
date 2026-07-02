package cz.jpetrla.garmin.demo.activity.exception;

public class DuplicateActivityException extends RuntimeException {

    public DuplicateActivityException(Long fitId) {
        super("Activity with fitId " + fitId + " already exists.");
    }
}
