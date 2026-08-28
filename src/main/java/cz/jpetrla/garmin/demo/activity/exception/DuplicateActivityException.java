package cz.jpetrla.garmin.demo.activity.exception;

public class DuplicateActivityException extends RuntimeException {

    public DuplicateActivityException() {
        super("Activity has already been imported.");
    }
}
