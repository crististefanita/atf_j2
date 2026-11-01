package atf.sql.query;

import java.util.List;
import java.util.Objects;

/** Prepared query template and ordered binds; values are never interpolated into SQL text. */
public record SqlQuery(String template, List<SqlParameter> parameters) {
    public SqlQuery {
        if (Objects.requireNonNull(template, "template").isBlank()) throw new IllegalArgumentException("SQL template required");
        parameters = List.copyOf(parameters);
    }
    public SqlQuery(String template) { this(template, List.of()); }
    @Override public String toString() { return "SqlQuery[template=captured, parameters=" + parameters.size() + "]"; }
}
