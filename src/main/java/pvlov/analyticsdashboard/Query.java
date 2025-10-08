package pvlov.analyticsdashboard;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("query")
public record Query(@Id long id, String text) {

    public Query(final String text) {
        this(0, text);
    }
}
