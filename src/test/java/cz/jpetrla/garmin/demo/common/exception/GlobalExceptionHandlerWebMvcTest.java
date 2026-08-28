package cz.jpetrla.garmin.demo.common.exception;

import cz.jpetrla.garmin.demo.activity.api.ActivityController;
import cz.jpetrla.garmin.demo.activity.application.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
class GlobalExceptionHandlerWebMvcTest {

    private static final String INTERNAL_DETAILS = "password=secret123 at /var/data/internal.db";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    void nonNumericActivityId_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/activities/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    @Test
    void unknownEndpoint_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("org.springframework"))));
    }

    @Test
    void unsupportedHttpMethod_returnsMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/activities/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("org.springframework"))));
    }

    @Test
    void unexpectedApplicationException_returnsInternalServerErrorWithoutLeakingDetails() throws Exception {
        when(activityService.getActivity(1L)).thenThrow(new RuntimeException(INTERNAL_DETAILS));

        mockMvc.perform(get("/api/activities/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
                .andExpect(content().string(not(containsString(INTERNAL_DETAILS))));
    }

    @Test
    void unexpectedIOException_returnsInternalServerErrorWithoutLeakingDetails() throws Exception {
        UncheckedIOException exception = new UncheckedIOException(
                "Failed to read uploaded file: /tmp/upload-8f3a.tmp",
                new IOException("disk error"));
        when(activityService.getActivity(3L)).thenThrow(exception);

        mockMvc.perform(get("/api/activities/3"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
                .andExpect(content().string(not(containsString("/tmp/upload-8f3a.tmp"))));
    }

    @Test
    void unexpectedIllegalArgumentException_returnsInternalServerErrorWithoutLeakingDetails() throws Exception {
        when(activityService.getActivity(2L)).thenThrow(new IllegalArgumentException(INTERNAL_DETAILS));

        mockMvc.perform(get("/api/activities/2"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."))
                .andExpect(content().string(not(containsString(INTERNAL_DETAILS))));
    }
}
