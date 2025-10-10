package pvlov.analyticsdashboard;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import pvlov.analyticsdashboard.util.IntegrationTestMixin;

import static org.junit.jupiter.api.Assertions.*;

// Technically this could also just be a slice test using @WebFluxTest, but for simplicity and uniformity
// we'll keep it as an integration test. Remember: The queries themselves are irrelevant, since the initial validation
// is a pure AST check using JSQLParser.
public class QueryValidationTest extends IntegrationTestMixin {

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "\n",
        "\t",
        " \n\t  "
    })
    void saveQuery__shouldReturnBadRequest__whenQueryIsEmpty(final String queryText) {
        getWebTestClient()
                .post()
                .uri("/query")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Query(queryText))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Query text must not be empty or blank.");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT FROM", // missing columns
        "SELECT * FROM", // missing table
        "SELCT * FROM users", // typo in SELECT
    })
    void saveQuery__shouldReturnBadRequest__whenQueryIsMalformed(final String queryText) {
        getWebTestClient()
                .post()
                .uri("/query")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Query(queryText))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .value(String.class, val -> assertTrue(val.startsWith("Syntax error in SQL query"), "Error message for malformed query should start with 'Syntax error in SQL query'"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INSERT INTO users (name) VALUES ('test')",
        "UPDATE users SET name = 'test' WHERE id = 1",
        "DELETE FROM users WHERE id = 1",
        "DROP TABLE users",
        "CREATE TABLE test (id INT)"
    })
    void saveQuery__shouldReturnBadRequest__whenQueryAttemptsToWrite(final String queryText) {
        getWebTestClient()
                .post()
                .uri("/query")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Query(queryText))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.message")
                .isEqualTo("Only SELECT statements are allowed.");
    }
}
