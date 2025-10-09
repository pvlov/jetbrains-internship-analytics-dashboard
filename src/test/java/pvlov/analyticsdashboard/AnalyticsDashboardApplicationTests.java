package pvlov.analyticsdashboard;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.class)
class AnalyticsDashboardApplicationTests {

    @LocalServerPort
    private int port;

    private TestWebClient testClient;

    @BeforeEach
    void setUp() {
        final String baseUrl = "http://localhost:" + port;
        testClient = new TestWebClient(baseUrl);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void test__healthCheckReturns200() {
        final var request = testClient.healthCheck();

        StepVerifier.create(request).expectNext("{\"status\":\"UP\"}").verifyComplete();
    }

    @Test
    void testSaveQuery() {
        String queryText = "SELECT * FROM users";
        final var request = testClient.saveQuery(queryText);

        StepVerifier.create(request).as("Should return the id of the saved query")
                .assertNext(id -> assertEquals(0, id, "Query ID should be 0 since it is the first inserted query"))
                .verifyComplete();
    }

    @Test
    void testGetAllQueries() {
        final var request = testClient.getAllQueries();

        StepVerifier.create(request).as("Should return an empty flux if no queries are saved").verifyComplete();
    }

    @Test
    void test__addingAndRetrievingQuery() {
        String queryText = "SELECT * FROM users";
        // Save the query
        testClient.saveQuery(queryText).block();

        final var queries = testClient.getAllQueries().map(Query::text).collectList().block();

        assertTrue(queries.contains(queryText));
    }

    @Test
    void testLongRunningQuery() {
        final var query = """
                SELECT
                    p1.name,
                    p2.name,
                    ABS(p1.age - p2.age) AS age_difference
                FROM
                    passenger AS p1
                CROSS JOIN
                    passenger AS p2
                WHERE
                    p1.id <> p2.id AND p1.fare > (p2.fare * 2);
                """;

        Long savedQueryId = testClient.saveQuery(query).block();

        assertNotNull(savedQueryId);

        final var result = testClient.executeQuery(savedQueryId).block();

        System.out.println(result);

        Mono.delay(Duration.ofSeconds(5)).block();

        final var notPending = testClient.executeQuery(savedQueryId).block();

        System.out.println(notPending);
    }
}
