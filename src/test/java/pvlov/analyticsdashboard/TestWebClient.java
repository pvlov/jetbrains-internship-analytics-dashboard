package pvlov.analyticsdashboard;

import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

/**
 * Wrapper-class around a {@link org.springframework.web.reactive.function.client.WebClient}
 * to make testing the API easier by providing higher-level methods to directly interact
 * with the API endpoints.
 */
public class TestWebClient {

    private final WebClient webClient;

    public TestWebClient(@NonNull final String baseUrl) {
        final int size = 16 * 1024 * 1024; // 16 MB

        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size)).build();

        this.webClient = WebClient.builder().baseUrl(baseUrl).exchangeStrategies(strategies).build();
    }

    @NonNull
    public Mono<String> healthCheck() {
        return webClient.get().uri("/actuator/health").retrieve().bodyToMono(String.class);
    }

    @NonNull
    public Mono<Long> saveQuery(@NonNull final String queryText) {
        return webClient.post().uri("/query").bodyValue(new Query(queryText)).retrieve().bodyToMono(Long.class);
    }

    @NonNull
    public Flux<Query> getAllQueries() {
        return webClient.get().uri("/query").retrieve().bodyToFlux(Query.class);
    }

    @NonNull
    public Mono<QueryResult> executeQuery(@NonNull final long queryId) {
        return webClient.get().uri("/query/{id}", queryId).retrieve().bodyToMono(QueryResult.class);
    }
}
