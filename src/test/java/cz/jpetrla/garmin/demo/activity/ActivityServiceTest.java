package cz.jpetrla.garmin.demo.activity;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.fit.FitActivityParser;
import cz.jpetrla.garmin.demo.activity.persistence.ActivityEntity;
import cz.jpetrla.garmin.demo.activity.persistence.ActivityRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    private static final String FIT_FILE_HASH =
            "039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81";

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
                .sport("RUNNING")
                .startedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    @Test
    void processActivity_rejectsNullFile() {
        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> activityService.processActivity(null));

        assertEquals("File is empty.", exception.getMessage());
    }

    @Test
    void processActivity_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[0]);

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> activityService.processActivity(empty));

        assertEquals("File is empty.", exception.getMessage());
    }

    @Test
    void processActivity_rejectsNonFitExtension() {
        MockMultipartFile wrongExtension = new MockMultipartFile("file", "activity.txt", "text/plain", new byte[]{1});

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> activityService.processActivity(wrongExtension));

        assertEquals("Only .fit files are supported.", exception.getMessage());
    }

    @Test
    void processActivity_wrapsIOExceptionFromFileReadIntoUncheckedIOException() throws IOException {
        MultipartFile brokenFile = mock(MultipartFile.class);
        when(brokenFile.isEmpty()).thenReturn(false);
        when(brokenFile.getOriginalFilename()).thenReturn("activity.fit");
        when(brokenFile.getInputStream()).thenThrow(new IOException("disk error"));

        UncheckedIOException exception = assertThrows(
                UncheckedIOException.class,
                () -> activityService.processActivity(brokenFile));

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void processActivity_rejectsSessionWithoutStartedAt() {
        when(fitActivityParser.parse(any(InputStream.class)))
                .thenReturn(ActivityData.builder().build());

        InvalidActivityFileException exception = assertThrows(
                InvalidActivityFileException.class,
                () -> activityService.processActivity(fitFile()));

        assertEquals("FIT file does not contain a session with a timestamp.", exception.getMessage());
    }

    @Test
    void processActivity_savesParsedActivityAndReturnsMappedResponse() {
        ActivityData data = activityData();
        ActivityEntity mappedEntity = ActivityEntity.builder().fileHash(FIT_FILE_HASH).build();
        ActivityEntity savedEntity = ActivityEntity.builder().id(1L).fileHash(FIT_FILE_HASH).build();
        ActivityResponse response = new ActivityResponse(1L, data.getSport(), null, null, null,
                data.getStartedAt(), null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFileHash(FIT_FILE_HASH)).thenReturn(false);
        when(activityMapper.toEntity(data, FIT_FILE_HASH)).thenReturn(mappedEntity);
        when(activityRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(activityMapper.toResponse(savedEntity)).thenReturn(response);

        ActivityResponse result = activityService.processActivity(fitFile());

        assertEquals(response, result);
    }

    @Test
    void processActivity_throwsDuplicateActivityExceptionWhenFileHashAlreadyExists() {
        ActivityData data = activityData();

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFileHash(FIT_FILE_HASH)).thenReturn(true);

        DuplicateActivityException exception = assertThrows(
                DuplicateActivityException.class,
                () -> activityService.processActivity(fitFile()));

        assertEquals("Activity has already been imported.", exception.getMessage());

        verify(activityRepository, never()).save(any());
    }

    @Test
    void processActivity_rethrowsDataIntegrityViolationsUnrelatedToUniqueConstraint() {
        ActivityData data = activityData();
        ActivityEntity mappedEntity = ActivityEntity.builder().fileHash(FIT_FILE_HASH).build();
        ConstraintViolationException notNullViolation = new ConstraintViolationException(
                "null value in column \"started_at\" violates not-null constraint",
                new SQLException("null value in column \"started_at\" violates not-null constraint", "23502"),
                ConstraintViolationException.ConstraintKind.NOT_NULL,
                "started_at");
        DataIntegrityViolationException unrelated =
                new DataIntegrityViolationException("not null violation", notNullViolation);

        when(fitActivityParser.parse(any(InputStream.class))).thenReturn(data);
        when(activityRepository.existsByFileHash(FIT_FILE_HASH)).thenReturn(false);
        when(activityMapper.toEntity(data, FIT_FILE_HASH)).thenReturn(mappedEntity);
        when(activityRepository.save(mappedEntity)).thenThrow(unrelated);

        DataIntegrityViolationException thrown = assertThrows(
                DataIntegrityViolationException.class,
                () -> activityService.processActivity(fitFile()));

        assertSame(unrelated, thrown);
    }

    @Test
    void getActivities_appendsStableIdSortToClientSort() {
        Pageable requestedPage = PageRequest.of(0, 20, Sort.by(Sort.Order.asc("sport")));
        when(activityRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        activityService.getActivities(requestedPage);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(activityRepository).findAll(pageableCaptor.capture());
        assertEquals(Sort.by(
                Sort.Order.asc("sport"),
                Sort.Order.desc("id")
        ), pageableCaptor.getValue().getSort());
    }
}
