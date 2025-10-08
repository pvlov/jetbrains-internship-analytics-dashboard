package pvlov.analyticsdashboard;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class QueryService {

    private final AsyncCache<String, QueryResult> queryResultCache;
    private final QueryRepository queryRepository;
    private final DatabaseClient databaseClient;

    @Autowired
    public QueryService(@Value("${app.caching.maxSize}") final int maxCacheSize, @Value("${app.caching.ttl}") final Duration cacheEntryTtl, final QueryRepository queryRepository, final DatabaseClient databaseClient) {
        this.queryResultCache =  Caffeine.newBuilder()
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
        return queryRepository.findById(queryId)
                .flatMap(query -> {
                    final var cachedValue = queryResultCache.get(query.text(),( queryText, _exec) -> runQueryInBackground(queryText));

                    // What this essentially does is: if the query result is already in the cache, return it immediately.
                    // If not, start executing the query in the background and return a "pending" status.
                    // We dont use Duration.ZERO in case this is a very fast query. Having the client poll again
                    // for something that was essentially done already would be punishing.
                    return Mono.fromFuture(cachedValue).timeout(Duration.ofMillis(300), Mono.just(QueryResult.pending()));
                });
    }

    public CompletableFuture<QueryResult> runQueryInBackground(final String queryText) {
        // Execute the query and update the cache with the result
        return databaseClient.sql(queryText)
                .fetch()
                .all()
                .collectList()
                .map(QueryResult::success)
                .onErrorResume(err -> Mono.just(QueryResult.failed(err.getMessage())))
                .toFuture();
    }
}
