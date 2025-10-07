package pvlov.analyticsdashboard;

import java.util.Map;

public sealed interface ExecutionResult permits ExecutionResult.Pending , ExecutionResult.Completed, ExecutionResult.Failed {

    static Pending pending() {
        return new Pending();
    }

    record Pending() implements ExecutionResult {}
    record Completed(Map<String, Object> result) implements ExecutionResult {}
    record Failed(String erroMessage) implements ExecutionResult {}
}
