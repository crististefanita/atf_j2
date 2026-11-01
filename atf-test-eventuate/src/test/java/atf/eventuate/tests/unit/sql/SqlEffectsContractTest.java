package atf.eventuate.tests.unit.sql;

import atf.eventuate.sql.*;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@AtfUnitTest
class SqlEffectsContractTest {
    private static final ReservationEffectContract EXPECTED = new ReservationEffectContract(42, new BigDecimal("70.00"), "APPROVED");
    private static SqlEffectsSnapshot.Reservation row(long order, String amount, String state) {
        return new SqlEffectsSnapshot.Reservation(1, order, new BigDecimal(amount), state);
    }
    private static SqlEffectsSnapshot snapshot(long requested, List<SqlEffectsSnapshot.Reservation> rows) {
        return new SqlEffectsSnapshot(requested, "fixture", SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, rows, Duration.ZERO, Optional.empty());
    }
    @Test void comparesCreditNumericallyWithoutDoubleCoercion() {
        assertTrue(EXPECTED.evaluate(snapshot(42, List.of(row(42, "70.0", "APPROVED")))).passed());
        var exact = new ReservationEffectContract(Long.MAX_VALUE, new BigDecimal("9007199254740993.01"), "APPROVED");
        assertTrue(exact.evaluate(snapshot(Long.MAX_VALUE, List.of(row(Long.MAX_VALUE, "9007199254740993.01", "APPROVED")))).passed());
        assertFalse(exact.evaluate(snapshot(Long.MAX_VALUE, List.of(row(Long.MAX_VALUE, "9007199254740992.01", "APPROVED")))).passed());
    }
    @Test void rejectsForeignRequestedAndReturnedIdentities() {
        assertFalse(EXPECTED.evaluate(snapshot(43, List.of(row(42, "70", "APPROVED")))).passed());
        assertFalse(EXPECTED.evaluate(snapshot(42, List.of(row(43, "70", "APPROVED")))).passed());
    }
    @Test void rejectsEmptyDuplicateCreditAndStateControls() {
        assertFalse(EXPECTED.evaluate(snapshot(42, List.of())).passed());
        assertFalse(EXPECTED.evaluate(snapshot(42, List.of(row(42, "35", "APPROVED"), row(42, "35", "APPROVED")))).passed());
        assertFalse(EXPECTED.evaluate(snapshot(42, List.of(row(42, "70.01", "APPROVED")))).passed());
        assertFalse(EXPECTED.evaluate(snapshot(42, List.of(row(42, "70", "PENDING")))).passed());
    }
    @Test void refusesPartialRowsEvenWhenEveryBusinessValueMatches() {
        var original = new SqlEffectsSnapshot.Failure("java.sql.SQLException", "read", "45000", 901, "cause", List.of("cleanup"));
        var failed = new SqlEffectsSnapshot(42, "fixture", SqlEffectsSnapshot.Provenance.SQL_EMBEDDED,
                List.of(row(42, "70", "APPROVED")), Duration.ZERO, Optional.of(original));
        var verdict = EXPECTED.evaluate(failed);
        assertFalse(verdict.complete()); assertTrue(verdict.identity()); assertTrue(verdict.singleEffect());
        assertTrue(verdict.credit()); assertTrue(verdict.state()); assertFalse(verdict.passed());
    }
    @Test void capturesImmutableRowsAndRejectsInvalidMetadata() {
        var rows = new ArrayList<SqlEffectsSnapshot.Reservation>(); rows.add(row(42, "70", "APPROVED"));
        var captured = snapshot(42, rows); rows.clear(); assertEquals(1, captured.reservations().size());
        assertThrows(UnsupportedOperationException.class, () -> captured.reservations().clear());
        assertThrows(IllegalArgumentException.class, () -> new SqlEffectsSnapshot(42, " ", SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, List.of(), Duration.ZERO, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new SqlEffectsSnapshot(42, "fixture", SqlEffectsSnapshot.Provenance.SQL_EMBEDDED, List.of(), Duration.ofNanos(-1), Optional.empty()));
    }
}
