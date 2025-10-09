package pvlov.analyticsdashboard;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class QueryService {

    private final AsyncCache<String, QueryResult> queryResultCache;
    private final QueryRepository queryRepository;
    private final DatabaseClient databaseClient;

    private static final Duration QUERY_GRACE_DURATION = Duration.ofMillis(300);

    @Autowired
    public QueryService(
            @Value("${app.caching.maxSize}") final int maxCacheSize,
            @Value("${app.caching.ttl}") final Duration cacheEntryTtl,
            final QueryRepository queryRepository,
            final DatabaseClient databaseClient
    ) {
        this.queryResultCache = Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(cacheEntryTtl)
                .buildAsync();

        this.queryRepository = queryRepository;
        this.databaseClient = databaseClient;
    }

    public Mono<Long> saveQuery(final String queryText) {
        return this.queryRepository.save(new Query(queryText)).map(Query::id);
    }

    public Flux<Query> getAllQueries() {
        return this.queryRepository.findAll();
    }

    public Mono<QueryResult> pollQuery(final long queryId) {
        return queryRepository.findById(queryId).flatMap(query -> {
            final var cachedValue = queryResultCache.get(query.text(), (queryText, _exec) -> runQueryInBackground(queryText));

            // If the query result is already in the cache, return it immediately.
            // If not, start executing the query in the background and return a "pending"
            // status. We don't use Duration.ZERO in case this is a very fast query. Having the
            // client poll again for something that was essentially done already would be quite punishing.
            // So we give it a small grace period to see if the query finishes quickly.
            return Mono.fromFuture(cachedValue).timeout(QUERY_GRACE_DURATION, Mono.just(QueryResult.pending()));
        });
    }

    public CompletableFuture<QueryResult> runQueryInBackground(final String queryText) {
        // Execute the query and update the cache with the result
        return databaseClient
                .sql(queryText)
                .fetch()
                .all()
                .collectList()
                .map(QueryResult::success)
                .onErrorResume(err -> Mono.just(QueryResult.failed(err.getMessage())))
                .toFuture();
    }
}
