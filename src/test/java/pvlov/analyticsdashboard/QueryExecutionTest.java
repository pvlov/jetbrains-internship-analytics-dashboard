package pvlov.analyticsdashboard;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import pvlov.analyticsdashboard.util.IntegrationTestMixin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class QueryExecutionTest extends IntegrationTestMixin {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "SELECT * FROM users",
            "SELECT * FROM passenger",
            "SELECT * FROM passenger where age > 30"
        }
    )
    void saveQuery__shouldReturnOk__whenGivenValidQueries(final String queryText) {
        getQueryTestClient().successFullySaveQuery(queryText);
    }

    @Test
    void getAllQueries__shouldReturnOk() {
        getQueryTestClient().successFullyGetAllQueries();
    }

    @Test
    void getAllQueries__shouldContainInsertedQueries_whenInsertingQueriesBeforeGetting() {
        final Set<String> testQueries = Set.of(
            "SELECT * FROM users",
            "SELECT * FROM passenger",
            "SELECT * FROM passenger where age > 30"
        );

        testQueries.forEach(getQueryTestClient()::successFullySaveQuery);

        final var actualQueries = getQueryTestClient()
                .successFullyGetAllQueries()
                .stream()
                .map(Query::text)
                .collect(Collectors.toUnmodifiableSet());

        for (final var query : testQueries) {
            assertTrue(actualQueries.contains(query));
        }
    }

    // First some tests to check the mechanisms of our API

    @ValueSource(
        strings = {
            """
            SELECT
                id, name, age
            FROM
                passenger
            WHERE
                pclass = 1 AND SLEEP(500) IS NULL
            LIMIT 1;
            """,
        }
    )
    @ParameterizedTest
    void executeQuery__shouldReturnPendingAndAfterSomeTimeSuccess__whenAskedToExecuteASlowQuery(final String queryText) throws InterruptedException {
        final var testClient = getQueryTestClient();

        long savedQueryId = testClient.successFullySaveQuery(queryText);
        final var pendingResult = testClient.successFullyExecuteQuery(savedQueryId);
        assertInstanceOf(QueryResult.Pending.class, pendingResult);

        while (true) {
            // Poll the API
            final var result = testClient.successFullyExecuteQuery(savedQueryId);

            if (result instanceof QueryResult.Pending) {
                // Wait some time before polling again
                Thread.sleep(200);
                continue;
            }

            // We got a non-pending result
            assertInstanceOf(QueryResult.Success.class, result);
            break;
        }
    }

    @ValueSource(
        strings = {
            """
            SELECT
                1
            FROM
                passenger
            """,
            }
    )
    @ParameterizedTest
    void executeQuery__shouldNotReturnPending__whenTheQueryRunsFast(final String queryText) {
        final var testClient = getQueryTestClient();
        long savedQueryId = testClient.successFullySaveQuery(queryText);
        final var result = testClient.successFullyExecuteQuery(savedQueryId);
        assertInstanceOf(QueryResult.Success.class, result);
    }

    @ValueSource(
            strings = {
                    """
                    SELECT
                        1
                    FROM
                        nonExistingTable
                    """,
            }
    )
    @ParameterizedTest
    void executeQuery__shouldReturnFailure__whenTheQueryCantBeProcessed(final String queryText) {
        final var testClient = getQueryTestClient();
        long savedQueryId = testClient.successFullySaveQuery(queryText);
        final var result = testClient.successFullyExecuteQuery(savedQueryId);
        assertInstanceOf(QueryResult.Failed.class, result);
    }

    private static Stream<Arguments> testQueryProvider() {
        return Stream.of(
            Arguments.of(
                    "SELECT READONLY();",
                    List.of(
                        Map.of(
                        "READONLY()", false
                        )
                    )
            ),
            Arguments.of(
                    """
                            SELECT
                                        name,
                                        age
                                    FROM
                                        passenger
                                    ORDER BY
                                        age DESC, name ASC
                                    LIMIT 5;
                    """,
                    List.of(
                        Map.of("NAME", "Alfred Henson", "AGE", 80.0),
                        Map.of("NAME", "Alisha Turner", "AGE", 80.0),
                        Map.of("NAME", "Amanda Moore", "AGE", 80.0),
                        Map.of("NAME", "Amy Lopez", "AGE", 80.0),
                        Map.of("NAME", "Andrew Bowen", "AGE", 80.0)
                    )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("testQueryProvider")
    void executeQuery__shouldReturnCorrectResults__whenGivenSensibleQueries(final String queryText, final List<Map<String, Object>> expectedResult) throws InterruptedException {
        final var testClient = getQueryTestClient();
        long savedQueryId = testClient.successFullySaveQuery(queryText);

        while (true) {
            final var result = testClient.successFullyExecuteQuery(savedQueryId);

            switch (result) {
                case QueryResult.Success(var ignored, var res) -> {
                    assertEquals(expectedResult, res, "Result set is not correct - db trouble?");
                    return;
                }
                // Wait some time before polling again
                case QueryResult.Pending(var ignored) -> Thread.sleep(200);
                case QueryResult.Failed(var ignored, var errorMessage) -> fail("Query unexpectedly failed! Reason: " + errorMessage);
            }
        }
    }


    @ValueSource(
            strings = {
                    """
                    SELECT
                        id, name, age
                    FROM
                        passenger
                    WHERE
                        pclass = 1 AND SLEEP(800) IS NULL
                    LIMIT 1;
                    """,
            }
    )
    @ParameterizedTest
    void executeQuery__shouldReturnSuccess__onASecondCallForAQueryEvenIfSlow(final String queryText) throws InterruptedException {
        final var testClient = getQueryTestClient();
        long savedQueryId = testClient.successFullySaveQuery(queryText);

        while (true) {
            final var result = testClient.successFullyExecuteQuery(savedQueryId);

            switch (result) {
                case QueryResult.Success(var ignored, var ignored2) -> {}
                // Wait some time before polling again
                case QueryResult.Pending(var ignored) -> Thread.sleep(200);
                case QueryResult.Failed(var ignored, var errorMessage) -> fail("Query unexpectedly failed! Reason: " + errorMessage);
            }

            if (result instanceof QueryResult.Success) break;
        }

        long cachedSavedQueryId = testClient.successFullySaveQuery(queryText);
        final var result = testClient.successFullyExecuteQuery(cachedSavedQueryId);
        assertInstanceOf(QueryResult.Success.class, result, "The second call should be cached!");
    }
}
