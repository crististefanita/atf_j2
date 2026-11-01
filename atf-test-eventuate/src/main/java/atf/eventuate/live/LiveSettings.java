package atf.eventuate.live;

import java.net.URI;
import java.time.Duration;
import java.util.List;

/** Opt-in laboratory configuration; loading it never starts or stops infrastructure. */
public record LiveSettings(URI orders, URI customers, String bootstrap, String jdbcUrl,
        String jdbcUser, String jdbcPassword, Duration runBudget, Duration readTimeout) {
    public static final String ORDER_TOPIC = "io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order";
    public static final String CUSTOMER_TOPIC = "io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer";
    public LiveSettings {
        for (var uri : List.of(orders, customers)) {
            if (!List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Explicit HTTP laboratory endpoint without credentials required");
        }
        if (!jdbcUrl.startsWith("jdbc:mysql://") || jdbcUser.isBlank() || bootstrap.isBlank())
            throw new IllegalArgumentException("Explicit Kafka and read-only MySQL settings required");
        if (runBudget.isNegative() || runBudget.isZero() || runBudget.compareTo(Duration.ofMinutes(2)) > 0
                || readTimeout.isNegative() || readTimeout.isZero() || readTimeout.compareTo(runBudget) > 0)
            throw new IllegalArgumentException("LIVE limits must be positive and bounded by two minutes");
    }
    public static LiveSettings load() {
        if (!Boolean.getBoolean("atf.eventuate.live.enabled")) throw new IllegalStateException("LIVE requires explicit opt-in");
        return new LiveSettings(URI.create(property("orders", "http://atf-j2-eventuate-orders:8080")),
                URI.create(property("customers", "http://atf-j2-eventuate-customers:8080")),
                property("bootstrap", "atf-j2-kafka:29092"),
                property("jdbc.url", "jdbc:mysql://atf-j2-eventuate-db:3306/eventuate?useSSL=false&allowPublicKeyRetrieval=true&useCursorFetch=true&useServerPrepStmts=true"),
                property("jdbc.user", "atf_reader"), property("jdbc.password", "atf-lab-reader"),
                Duration.ofMillis(Long.parseLong(property("budget.ms", "15000"))),
                Duration.ofMillis(Long.parseLong(property("read.ms", "2000"))));
    }
    private static String property(String name, String fallback) { return System.getProperty("atf.eventuate.live." + name, fallback); }
    @Override public String toString() { return "LiveSettings[orders=" + orders + ", customers=" + customers + ", credentials=redacted]"; }
}
