package pvlov.analyticsdashboard;


import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface QueryRepository extends ReactiveCrudRepository<Query, Long> {}
