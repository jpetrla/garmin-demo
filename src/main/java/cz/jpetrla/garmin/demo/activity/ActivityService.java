package cz.jpetrla.garmin.demo.activity;

import cz.jpetrla.garmin.demo.activity.api.ActivityResponse;
import cz.jpetrla.garmin.demo.activity.fit.FitActivityParser;
import cz.jpetrla.garmin.demo.activity.persistence.ActivityEntity;
import cz.jpetrla.garmin.demo.activity.persistence.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final FitActivityParser fitActivityParser;
    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    @Transactional
    public ActivityResponse processActivity(MultipartFile file) {
        validateFile(file);

        ActivityData activityData;
        MessageDigest fileDigest = sha256Digest();
        try (InputStream hashingStream = new DigestInputStream(file.getInputStream(), fileDigest)) {
            activityData = fitActivityParser.parse(hashingStream);

            // Ensure the hash covers the whole file even if the parser stops reading early.
            hashingStream.transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
        String fileHash = HexFormat.of().formatHex(fileDigest.digest());

        if (activityData.getStartedAt() == null) {
            throw new InvalidActivityFileException("FIT file does not contain a session with a timestamp.");
        }

        if (activityRepository.existsByFileHash(fileHash)) {
            throw new DuplicateActivityException();
        }

        // The check above is a fast pre-check for a clear error message in the common case, but it
        // can't prevent a race between two concurrent uploads of the same file; the unique DB
        // constraint is the actual safety net, surfaced here as a DataIntegrityViolationException.
        try {
            ActivityEntity saved = activityRepository.save(activityMapper.toEntity(activityData, fileHash));
            return activityMapper.toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new DuplicateActivityException();
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Page<ActivityResponse> getActivities(Pageable pageable) {
        if (pageable.isPaged() && pageable.getSort().getOrderFor("id") == null) {
            Sort stableSort = pageable.getSort().and(Sort.by(Sort.Order.desc("id")));
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), stableSort);
        }
        return activityRepository.findAll(pageable).map(activityMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ActivityResponse getActivity(Long id) {
        ActivityEntity entity = activityRepository.findById(id).orElseThrow(() -> new ActivityNotFoundException(id));
        return activityMapper.toResponse(entity);
    }

    @Transactional
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new ActivityNotFoundException(id);
        }
        activityRepository.deleteById(id);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidActivityFileException("File is empty.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".fit")) {
            throw new InvalidActivityFileException("Only .fit files are supported.");
        }
    }

    // file_hash carries the only unique constraint on the table besides the primary key, so the
    // constraint kind is enough to identify a duplicate upload. Narrowing this by constraint name
    // would mean relying on Hibernate parsing the name out of the driver's message text, which is
    // locale-dependent; revisit only if a second unique constraint is ever added here.
    private static boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }
}
