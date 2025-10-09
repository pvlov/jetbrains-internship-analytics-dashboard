package pvlov.analyticsdashboard;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "state",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = QueryResult.Pending.class, name = "PENDING"),
        @JsonSubTypes.Type(value = QueryResult.Success.class, name = "COMPLETED"),
        @JsonSubTypes.Type(value = QueryResult.Failed.class, name = "FAILED")
})
public sealed interface QueryResult permits QueryResult.Pending, QueryResult.Success, QueryResult.Failed {

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
