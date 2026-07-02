package cz.jpetrla.garmin.demo.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleUnexpectedException_returnsGenericMessageWithoutLeakingExceptionDetails() {
        RuntimeException ex = new RuntimeException("password=secret123 at /var/data/internal.db");

        ResponseEntity<String> response = handler.handleUnexpectedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .doesNotContain("password=secret123", "/var/data/internal.db")
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }

    @Test
    void handleUncheckedIO_returnsGenericMessageWithoutLeakingExceptionDetails() {
        UncheckedIOException ex = new UncheckedIOException(
                "Failed to read uploaded file: /tmp/upload-8f3a.tmp", new IOException("disk error"));

        ResponseEntity<String> response = handler.handleUncheckedIO(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .doesNotContain("/tmp/upload-8f3a.tmp")
                .isEqualTo("An unexpected error occurred. Please try again later.");
    }
}
