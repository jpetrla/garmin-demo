package cz.jpetrla.garmin.demo.common.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(configurations.get("/api/**").getAllowedOrigins())
                .containsExactly("http://localhost:5173", "https://app.example.com");
    }
}
