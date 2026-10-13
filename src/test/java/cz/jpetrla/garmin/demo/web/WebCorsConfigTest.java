package cz.jpetrla.garmin.demo.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebCorsConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    void corsConfigurer_registersEveryConfiguredOriginSeparately() {
        WebCorsConfig config = new WebCorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins",
                new String[]{"http://localhost:5173", "https://app.example.com"});

        WebMvcConfigurer configurer = config.corsConfigurer();
        CorsRegistry registry = new CorsRegistry();
        configurer.addCorsMappings(registry);

        Map<String, CorsConfiguration> configurations =
                (Map<String, CorsConfiguration>) ReflectionTestUtils.invokeMethod(registry, "getCorsConfigurations");

        assertEquals(
                List.of("http://localhost:5173", "https://app.example.com"),
                configurations.get("/api/**").getAllowedOrigins());
    }
}
