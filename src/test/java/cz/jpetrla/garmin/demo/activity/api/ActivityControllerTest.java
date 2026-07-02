package cz.jpetrla.garmin.demo.activity.api;

import cz.jpetrla.garmin.demo.activity.application.ActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityControllerTest {

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private ActivityController activityController;

    private static ActivityResponse response(Long id) {
        return new ActivityResponse(
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void upload_returnsCreatedWithLocationHeaderAndMappedBody() {
        MultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[]{1});
        ActivityResponse response = response(42L);

        when(activityService.processActivity(file)).thenReturn(response);

        ResponseEntity<ActivityResponse> result = activityController.upload(file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getHeaders().getLocation()).isEqualTo(URI.create("/api/activities/42"));
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void getActivities_returnsOkWithPagedResponses() {
        ActivityResponse response = response(1L);
        Pageable pageable = PageRequest.of(0, 20);
        Page<ActivityResponse> page = new PageImpl<>(List.of(response), pageable, 1);

        when(activityService.getActivities(pageable)).thenReturn(page);

        ResponseEntity<PagedModel<ActivityResponse>> result = activityController.getActivities(pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getContent()).containsExactly(response);
    }

    @Test
    void getActivity_returnsOkWithMappedResponse() {
        ActivityResponse response = response(7L);

        when(activityService.getActivity(7L)).thenReturn(response);

        ResponseEntity<ActivityResponse> result = activityController.getActivity(7L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }

    @Test
    void deleteActivity_delegatesToServiceAndReturnsNoContent() {
        ResponseEntity<Void> result = activityController.deleteActivity(3L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(activityService).deleteActivity(3L);
    }
}
