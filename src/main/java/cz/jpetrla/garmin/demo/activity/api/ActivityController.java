package cz.jpetrla.garmin.demo.activity.api;

import cz.jpetrla.garmin.demo.activity.application.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ActivityResponse> upload(@RequestPart("file") MultipartFile file) {
        ActivityResponse response = activityService.processActivity(file);
        URI location = URI.create("/api/activities/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedModel<ActivityResponse>> getActivities(
            @PageableDefault(size = 20, sort = {"startedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(activityService.getActivities(pageable)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ActivityResponse> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(activityService.getActivity(id));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}
