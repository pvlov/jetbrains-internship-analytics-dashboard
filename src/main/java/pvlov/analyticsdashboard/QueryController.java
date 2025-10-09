package pvlov.analyticsdashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
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
