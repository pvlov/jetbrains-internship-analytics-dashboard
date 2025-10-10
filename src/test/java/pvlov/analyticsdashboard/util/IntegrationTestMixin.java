package pvlov.analyticsdashboard.util;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// By default, Spring Boot does not include error messages in the response body for security reasons.
@TestPropertySource(properties = "server.error.include-message=always")
public class IntegrationTestMixin {

    @Autowired
    private WebTestClient webTestClient;

    private QueryTestClient queryTestClient;

    @BeforeEach
    void setUp() {
        this.queryTestClient = new QueryTestClient(webTestClient);
    }

    protected WebTestClient getWebTestClient() {
        return webTestClient;
    }

    protected QueryTestClient getQueryTestClient() {
        return queryTestClient;
    }
}
