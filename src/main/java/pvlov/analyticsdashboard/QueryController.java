package pvlov.analyticsdashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("query")
public class QueryController {

    private final DatabaseClient databaseClient;
    private final QueryRepository queryRepository;

    @Autowired
    public QueryController(final DatabaseClient databaseClient, final QueryRepository queryRepository) {
        this.databaseClient = databaseClient;
        this.queryRepository = queryRepository;
    }

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Long> saveQuery(@RequestBody final Query queryRequest) {
        return this.queryRepository.save(queryRequest).map(Query::id);
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Query> getAllQueries() {
        return this.queryRepository.findAll();
    }

    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Map<String, Object>> executeQuery(@PathVariable final long id) {
        return this.queryRepository.findById(id)
                .flatMapMany(query ->
                        databaseClient.sql(query::query)
                                .fetch()
                                .all()
                );
    }
}
