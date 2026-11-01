package atf.eventuate.sql;

import java.sql.SQLException;

/** Independent read of captured SQL effects; no writes, retry or business handler. */
@FunctionalInterface
public interface OrderEffectsReader {
    SqlEffectsSnapshot read(long orderId) throws SQLException;
}
