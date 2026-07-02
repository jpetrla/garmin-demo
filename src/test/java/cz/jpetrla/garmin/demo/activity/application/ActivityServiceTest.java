package cz.jpetrla.garmin.demo.activity.application;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.domain.ActivityData;
import cz.jpetrla.garmin.demo.activity.exception.ActivityNotFoundException;
import cz.jpetrla.garmin.demo.activity.exception.DuplicateActivityException;
import cz.jpetrla.garmin.demo.activity.infrastructure.fit.FitActivityParser;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityEntity;
import cz.jpetrla.garmin.demo.activity.infrastructure.repository.ActivityRepository;
import cz.jpetrla.garmin.demo.activity.mapper.ActivityMapper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private FitActivityParser fitActivityParser;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private ActivityService activityService;

    private static MockMultipartFile fitFile() {
        return new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[]{1, 2, 3});
    }

    private static ActivityData activityData() {
        return ActivityData.builder()
                .fitId(123L)
                .sport("RUNNING")
                .startedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void processActivity_rejectsNullFile() {
        assertThatThrownBy(() -> activityService.processActivity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty.");
    }

    @Test
    void processActivity_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[0]);

        assertThatThrownBy(() -> activityService.processActivity(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File is empty.");
    }

    @Test
    void processActivity_rejectsNonFitExtension() {
        MockMultipartFile wrongExtension = new MockMultipartFile("file", "activity.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> activityService.processActivity(wrongExtension))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only .fit files are supported.");
    }

    @Test
    void processActivity_wrapsIOExceptionFromFileReadIntoUncheckedIOException() throws IOException {
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getOriginalFilename()).thenReturn("activity.fit");
        when(brokenFile.getInputStream()).thenThrow(new IOException("disk error"));

        assertThatThrownBy(() -> activityService.processActivity(brokenFile))
                .isInstanceOf(UncheckedIOException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void processActivity_rejectsSessionWithoutFitId() {
        when(fitActivityParser.parse(any(InputStream.class)))
                .thenReturn(ActivityData.builder().startedAt(Instant.now()).build());

        assertThatThrownBy(() -> activityService.processActivity(fitFile()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FIT file does not contain a session with a timestamp.");
    }

    @Test
    void processActivity_rejectsSessionWithoutStartedAt() {
        when(fitActivityParser.parse(any(InputStream.class)))
                .thenReturn(ActivityData.builder().fitId(1L).build());

        assertThatThrownBy(() -> activityService.processActivity(fitFile()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("FIT file does not contain a session with a timestamp.");
    }

    @Test
    void processActivity_savesParsedActivityAndReturnsMappedResponse() {
        ActivityData data = activityData();
        ActivityEntity mappedEntity = ActivityEntity.builder().fitId(data.getFitId()).build();
        ActivityEntity savedEntity = ActivityEntity.builder().id(1L).fitId(data.getFitId()).build();
        ActivityResponse response = new ActivityResponse(1L, data.getFitId(), data.getSport(), null, null, null,
                data.getStartedAt(), null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFitId(data.getFitId())).thenReturn(false);
        when(activityMapper.toEntity(data)).thenReturn(mappedEntity);
        when(activityRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(activityMapper.toResponse(savedEntity)).thenReturn(response);

        ActivityResponse result = activityService.processActivity(fitFile());

        assertThat(result).isEqualTo(response);
    }

    @Test
    void processActivity_throwsDuplicateActivityExceptionWhenFitIdAlreadyExists() {
        ActivityData data = activityData();

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFitId(data.getFitId())).thenReturn(true);

        assertThatThrownBy(() -> activityService.processActivity(fitFile()))
                .isInstanceOf(DuplicateActivityException.class)
                .hasMessage("Activity with fitId " + data.getFitId() + " already exists.");

        verify(activityRepository, never()).save(any());
    }

    @Test
    void processActivity_translatesConcurrentUniqueConstraintViolationIntoDuplicateActivityException() {
        ActivityData data = activityData();
        ActivityEntity mappedEntity = ActivityEntity.builder().fitId(data.getFitId()).build();
        ConstraintViolationException uniqueViolation = new ConstraintViolationException(
                "duplicate key value violates unique constraint",
                new SQLException("duplicate key value violates unique constraint", "23505"),
                ConstraintViolationException.ConstraintKind.UNIQUE,
                "uq_activity_fit_id");

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFitId(data.getFitId())).thenReturn(false);
        when(activityMapper.toEntity(data)).thenReturn(mappedEntity);
        when(activityRepository.save(mappedEntity))
                .thenThrow(new DataIntegrityViolationException("constraint violated", uniqueViolation));

        assertThatThrownBy(() -> activityService.processActivity(fitFile()))
                .isInstanceOf(DuplicateActivityException.class)
                .hasMessage("Activity with fitId " + data.getFitId() + " already exists.");
    }

    @Test
    void processActivity_rethrowsDataIntegrityViolationsUnrelatedToUniqueConstraint() {
        ActivityData data = activityData();
        ActivityEntity mappedEntity = ActivityEntity.builder().fitId(data.getFitId()).build();
        ConstraintViolationException notNullViolation = new ConstraintViolationException(
                "null value in column \"started_at\" violates not-null constraint",
                new SQLException("null value in column \"started_at\" violates not-null constraint", "23502"),
                ConstraintViolationException.ConstraintKind.NOT_NULL,
                "started_at");
        DataIntegrityViolationException unrelated =
                new DataIntegrityViolationException("not null violation", notNullViolation);

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFitId(data.getFitId())).thenReturn(false);
        when(activityMapper.toEntity(data)).thenReturn(mappedEntity);
        when(activityRepository.save(mappedEntity)).thenThrow(unrelated);

        assertThatThrownBy(() -> activityService.processActivity(fitFile()))
                .isSameAs(unrelated);
    }

    @Test
    void getActivities_returnsMappedPageFromRepository() {
        ActivityEntity entity = ActivityEntity.builder().id(1L).build();
        ActivityResponse response = new ActivityResponse(1L, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(activityRepository.findAll(pageable)).thenReturn(entityPage);
        when(activityMapper.toResponse(entity)).thenReturn(response);

        assertThat(activityService.getActivities(pageable)).containsExactly(response);
    }

    @Test
    void getActivity_returnsMappedResponseWhenFound() {
        ActivityEntity entity = ActivityEntity.builder().id(1L).build();
        ActivityResponse response = new ActivityResponse(1L, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        when(activityRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(activityMapper.toResponse(entity)).thenReturn(response);

        assertThat(activityService.getActivity(1L)).isEqualTo(response);
    }

    @Test
    void getActivity_throwsActivityNotFoundExceptionWhenMissing() {
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> activityService.getActivity(1L))
                .isInstanceOf(ActivityNotFoundException.class)
                .hasMessage("Activity with id 1 not found.");
    }

    @Test
    void deleteActivity_deletesByIdWhenFound() {
        when(activityRepository.existsById(1L)).thenReturn(true);

        activityService.deleteActivity(1L);

        verify(activityRepository).deleteById(1L);
    }

    @Test
    void deleteActivity_throwsAndNeverDeletesWhenMissing() {
        when(activityRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> activityService.deleteActivity(1L))
                .isInstanceOf(ActivityNotFoundException.class);

        verify(activityRepository, never()).deleteById(any());
    }
}
