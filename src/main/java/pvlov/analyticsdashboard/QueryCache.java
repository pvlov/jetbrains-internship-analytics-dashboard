package pvlov.analyticsdashboard;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.function.Function;

public class QueryCache {

    // Maps a text string to its result
    private final AsyncCache<String, QueryResult> cache;

    public QueryCache(final long maximumSize, final Duration ttl) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(ttl)
                .buildAsync();
    }

    public Mono<QueryResult> fetchThroughCache(final String query, final Function<String, QueryResult> computeFunction) {
        return Mono.fromFuture(cache.get(query, computeFunction));
    }

}
