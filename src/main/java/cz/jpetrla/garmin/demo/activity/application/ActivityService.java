package cz.jpetrla.garmin.demo.activity.application;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.infrastructure.fit.FitActivityParser;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityEntity;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityRepository;
import cz.jpetrla.garmin.demo.activity.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cz.jpetrla.garmin.demo.activity.exception.ActivityNotFoundException;
import cz.jpetrla.garmin.demo.activity.exception.DuplicateActivityException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final FitActivityParser fitActivityParser;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    public ActivityResponse processActivity(MultipartFile file) {
        validateFile(file);

        ActivityData activityData;
        try (InputStream inputStream = file.getInputStream()) {
            activityData = fitActivityParser.parse(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }

        if (activityData.getFitId() == null || activityData.getStartedAt() == null) {
            throw new IllegalArgumentException("FIT file does not contain a session with a timestamp.");
        }

        if (activityRepository.existsByFitId(activityData.getFitId())) {
            throw new DuplicateActivityException(activityData.getFitId());
        }

        ActivityEntity saved = activityRepository.save(activityMapper.toEntity(activityData));
        return activityMapper.toResponse(saved);
    }

    public Page<ActivityResponse> getActivities(Pageable pageable) {
        return activityRepository.findAll(pageable).map(activityMapper::toResponse);
    }

    public ActivityResponse getActivity(Long id) {
        ActivityEntity entity = activityRepository.findById(id).orElseThrow(() -> new ActivityNotFoundException(id));
        return activityMapper.toResponse(entity);
    }

    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new ActivityNotFoundException(id);
        }
        activityRepository.deleteById(id);
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
