package pvlov.analyticsdashboard;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExecutionStore {

    private final Map<String, QueryResult> store;

    public ExecutionStore() {
        this.store = new ConcurrentHashMap<>();
    }

    public UUID submitJob(final String query) {
        final var id = UUID.randomUUID();
        store.put(id.toString(), QueryResult.pending());

        return id;
    }
}
