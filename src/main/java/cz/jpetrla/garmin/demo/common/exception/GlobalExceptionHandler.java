package cz.jpetrla.garmin.demo.common.exception;

import cz.jpetrla.garmin.demo.activity.exception.ActivityNotFoundException;
import cz.jpetrla.garmin.demo.activity.exception.DuplicateActivityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.UncheckedIOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(UncheckedIOException.class)
    public ResponseEntity<String> handleUncheckedIO(UncheckedIOException ex) {
        log.error("Unexpected I/O error while handling request", ex);
        return ResponseEntity.internalServerError().body(GENERIC_ERROR_MESSAGE);
    }

    @ExceptionHandler(ActivityNotFoundException.class)
    public ResponseEntity<String> handleActivityNotFound(ActivityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DuplicateActivityException.class)
    public ResponseEntity<String> handleDuplicateActivity(DuplicateActivityException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpectedException(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        return ResponseEntity.internalServerError().body(GENERIC_ERROR_MESSAGE);
    }
}
