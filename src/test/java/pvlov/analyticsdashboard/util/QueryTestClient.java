package pvlov.analyticsdashboard.util;

import io.micrometer.common.lang.NonNullApi;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import pvlov.analyticsdashboard.Query;
import pvlov.analyticsdashboard.QueryResult;

import java.util.List;

import static org.springframework.test.util.AssertionErrors.*;

/**
 * Wrapper-class around a {@link org.springframework.test.web.reactive.server.WebTestClient}
 * to make testing the API easier by providing higher-level methods to directly interact
 * with the API endpoints. The higher-level methods e.g. {@link #successFullyGetAllQueries()} will also
 * do some basic status and body checks to ensure the wiring/contract of the API is correct so
 * the tests using this client can focus on the actual test logic.
 */
@NonNullApi
public class QueryTestClient {

    private final WebTestClient webClient;

    public QueryTestClient(final WebTestClient client) {
        final int size = 16 * 1024 * 1024; // 16 MB

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build();

        this.webClient = client.mutate()
                .exchangeStrategies(strategies)
                .build();
    }

    public Long successFullySaveQuery(final String queryText) {
        final var id = webClient
                .post()
                .uri("/query")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Query(queryText))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Long.class)
                .returnResult()
                .getResponseBody();

        assertNotNull("ID response body should not be null", id);
        assert id != null; // satisfy the linter

        return id;
    }

    public List<Query> successFullyGetAllQueries() {
        final var list = webClient
                .get()
                .uri("/query")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Query.class)
                .returnResult()
                .getResponseBody();

        assertNotNull( "List of queries should not be null", list);
        assert list != null; // satisfy the linter

        return list;
    }

    public QueryResult successFullyExecuteQuery(final long queryId) {
        final var result = webClient
                .get()
                .uri("/query/{id}", queryId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(QueryResult.class)
                .returnResult()
                .getResponseBody();

        assertNotNull( "QueryResult should not be null", result);
        assert result != null; // satisfy the linter

        return result;
    }
}
