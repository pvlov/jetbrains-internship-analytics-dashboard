package pvlov.analyticsdashboard;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionStore {

    private final Map<String, ExecutionResult> store;

    public ExecutionStore() {
        this.store = new ConcurrentHashMap<>();
    }

    public UUID submitJob(final String query) {
        final var id = UUID.randomUUID();
        store.put(id.toString(), ExecutionResult.pending());




        queryRepository.findById(queryId)
                .flatMap(query ->
                        databaseClient.sql(query.getQueryText())
                                .fetch()
                                .all() // Returns Flux<Map<String, Object>>
                                .collectList() // Collect results into a list
                                .doOnSuccess(resultData ->
                                        // On success, update the store with the result
                                        executionStore.results.put(executionId, ExecutionResult.completed(resultData))
                                )
                                .doOnError(error ->
                                        // On error, update the store with the failure
                                        executionStore.results.put(executionId, ExecutionResult.failed(error.getMessage()))
                                )
                )
                .then();


        return id;
    }
}
