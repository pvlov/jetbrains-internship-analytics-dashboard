package pvlov.analyticsdashboard;

import io.vavr.control.Try;
import net.sf.jsqlparser.parser.AbstractJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("query")
public class QueryController {

    private final QueryService queryService;

    @Autowired
    public QueryController(final QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping(
            value = "",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Long> saveQuery(@RequestBody final Query queryRequest) {

        if (queryRequest == null || queryRequest.text() == null || queryRequest.text().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query text must not be empty or blank."));
        }

        final Try<Boolean> isLikelyReadOnly = Try.of(() -> CCJSqlParserUtil.parse(queryRequest.text(), AbstractJSqlParser::withAllowComplexParsing))
                .map(Select.class::isInstance);

        switch (isLikelyReadOnly) {
            case Try.Failure<Boolean> failure -> {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Syntax error in SQL query: " + failure.getCause().getMessage()));
            }
            case Try.Success<Boolean> success when !success.get() -> {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only SELECT statements are allowed."));
            }
            default -> {}
        }

        return this.queryService.saveQuery(queryRequest.text());
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Query> getAllQueries() {
        return this.queryService.getAllQueries();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<QueryResult> executeQuery(@PathVariable final long id) {
        return this.queryService.pollQuery(id);
    }
}
