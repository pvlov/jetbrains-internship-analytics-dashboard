package pvlov.analyticsdashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("query")
public class QueryController {

    private final DatabaseClient databaseClient;

    @Autowired
    public QueryController(final DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @PostMapping(value = "/execute", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Map<String, Object>> getAllPassengers(@RequestParam final String query) {
        return databaseClient.sql(query).fetch().all();
    }
}
