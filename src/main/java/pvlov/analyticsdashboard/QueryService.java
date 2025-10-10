package pvlov.analyticsdashboard;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class QueryService {

    private final Cache<String, Mono<QueryResult>> queryResultCache;
    private final QueryRepository queryRepository;
    private final DatabaseClient databaseClient;

    private final Duration graceQueryTimeout;
    private final Duration maxQueryTimeout;

    @Autowired
    public QueryService(
            @Value("${app.caching.maxSize}") final int maxCacheSize,
            @Value("${app.caching.ttl}") final Duration cacheEntryTtl,
            @Value("${app.graceQueryTimeout}") final Duration graceQueryTimeout,
            @Value("${app.maxQueryTimeout}") final Duration maxQueryTimout,
            final QueryRepository queryRepository,
            final DatabaseClient databaseClient
    ) {
        this.queryRepository = queryRepository;
        this.databaseClient = databaseClient;
        this.graceQueryTimeout = graceQueryTimeout;
        this.maxQueryTimeout = maxQueryTimout;

        this.queryResultCache = Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(cacheEntryTtl)
                .build();
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
                    // If the query result is already in the cache, return it immediately.
                    // If not, start executing the query in the background and return a "pending"
                    // status. We don't use Duration.ZERO in case this is a very fast query. Having the
                    // client poll again for something that was essentially done already would be quite punishing.
                    // So we give it a small grace period to see if the query finishes quickly.
                    // To achieve this, we just let a delay mono and the mono computing the result race against
                    // each other.
                    Mono<QueryResult> resultMono = this.queryResultCache.asMap().computeIfAbsent(
                            query.text(),
                            key -> runQueryInBackground(key)
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .cache()
                    );

                    Mono<QueryResult> pendingMono = Mono.delay(graceQueryTimeout)
                            .map(ignored -> QueryResult.pending());

                    return Mono.firstWithSignal(resultMono, pendingMono);
                });
    }

    public Mono<QueryResult> runQueryInBackground(final String queryText) {
        return databaseClient
                .sql(queryText)
                .fetch()
                .all()
                .collectList()
                .map(QueryResult::success)
                .timeout(maxQueryTimeout, Mono.just(QueryResult.failed("Query took too long!")))
                .onErrorResume(err -> Mono.just(QueryResult.failed(err.getMessage())))
                .doOnSuccess(result -> queryResultCache.put(queryText, Mono.just(result)));
    }
}
