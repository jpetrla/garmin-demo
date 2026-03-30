package cz.jpetrla.garmin.demo.activity.api;

import cz.jpetrla.garmin.demo.activity.application.ActivityService;
import cz.jpetrla.garmin.demo.activity.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ActivityUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(activityMapper.toResponse(activityService.processActivity(file)));
    }
}
