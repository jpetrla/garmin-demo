package cz.jpetrla.garmin.demo.activity.application;

import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.infrastructure.fit.FitActivityParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final FitActivityParser fitActivityParser;

    public ActivityData processActivity(MultipartFile file) {
        validateFile(file);

        try (InputStream inputStream = file.getInputStream()) {
            return fitActivityParser.parse(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".fit")) {
            throw new IllegalArgumentException("Only .fit files are supported.");
        }
    }
}
