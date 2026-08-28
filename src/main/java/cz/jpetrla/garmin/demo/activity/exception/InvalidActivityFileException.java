package cz.jpetrla.garmin.demo.activity.exception;

public class InvalidActivityFileException extends RuntimeException {

    public InvalidActivityFileException(String message) {
        super(message);
    }

    public InvalidActivityFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
