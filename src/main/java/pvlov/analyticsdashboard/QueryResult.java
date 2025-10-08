package pvlov.analyticsdashboard;

import java.util.List;
import java.util.Map;

public sealed interface QueryResult permits QueryResult.Pending , QueryResult.Success, QueryResult.Failed {

    static QueryResult pending() {
        return new Pending(QueryResultState.PENDING);
    }
    static QueryResult success(final List<Map<String, Object>> result) {
        return new Success(QueryResultState.COMPLETED, result);
    }

    static QueryResult failed(final String message) {
        return new Failed(QueryResultState.FAILED, message);
    }

    record Pending(QueryResultState state) implements QueryResult {}
    record Success(QueryResultState state, List<Map<String, Object>> result) implements QueryResult {}
    record Failed(QueryResultState state, String erroMessage) implements QueryResult {}
}
