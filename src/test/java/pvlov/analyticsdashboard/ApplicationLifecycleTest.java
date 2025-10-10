package pvlov.analyticsdashboard;


import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pvlov.analyticsdashboard.util.IntegrationTestMixin;

import static org.springframework.test.util.AssertionErrors.assertEquals;

public class ApplicationLifecycleTest extends IntegrationTestMixin {

    @Test
    void contextLoads() {}

    @Test
    void healthCheck__should__returnStatusUp__always() {
        getWebTestClient()
                .get()
                .uri("/actuator/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .value(String.class, status -> assertEquals("Health Check for Application should always succeed.", "UP", status));
    }
}
