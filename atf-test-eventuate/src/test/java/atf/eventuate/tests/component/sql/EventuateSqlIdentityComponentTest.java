package atf.eventuate.tests.component.sql;

import atf.eventuate.evidence.EventuateEvidence;
import atf.eventuate.sql.ReservationEffectContract;
import atf.eventuate.tests.support.EventuateComponentSupport;
import atf.eventuate.tests.support.EventuateComponentTest;
import atf.eventuate.tests.support.sql.H2SqlFixture;
import atf.testkit.assertions.RecordedAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

@EventuateComponentTest
public class EventuateSqlIdentityComponentTest extends EventuateComponentSupport {
    @Test @DisplayName("SQL embedded · identity, duplicate and exact decimal controls")
    void rejectsWrongEffectsWithoutImplementingDeduplication(RecordedAssertions checks, EventuateEvidence evidence) throws Exception {
        try (var db = new H2SqlFixture(); var other = new H2SqlFixture()) {
            var reader = db.reader(evidence);
            var expected = new ReservationEffectContract(42, new BigDecimal("70.00"), "APPROVED");
            db.reserve(1, 43, "70.00", "APPROVED"); db.commit();
            checks.isFalse("No row is not a single effect", expected.evaluate(reader.read(42)).passed());
            checks.isFalse("Foreign order cannot satisfy expected identity", expected.evaluate(reader.read(43)).passed());
            db.reserve(2, 42, "70.00", "APPROVED"); db.commit();
            var single = reader.read(42);
            checks.isTrue("Nominal single reservation passes", expected.evaluate(single).passed());
            db.reserve(3, 42, "70.00", "APPROVED"); db.commit();
            var duplicate = reader.read(42);
            checks.equals("Duplicate rows remain observable", 2, duplicate.reservations().size());
            checks.isFalse("Duplicate business effects are rejected", expected.evaluate(duplicate).passed());
            db.reserve(4, 44, "70.01", "APPROVED"); db.reserve(5, 45, "70.00", "PENDING");
            db.reserve(6, 46, "9007199254740993.01", "APPROVED"); db.commit();
            checks.isFalse("Incorrect credit is rejected", new ReservationEffectContract(44, new BigDecimal("70.00"), "APPROVED").evaluate(reader.read(44)).passed());
            checks.isFalse("Pending state is rejected", new ReservationEffectContract(45, new BigDecimal("70.00"), "APPROVED").evaluate(reader.read(45)).passed());
            var large = reader.read(46);
            checks.equals("Large decimal is not converted to Double", new BigDecimal("9007199254740993.01"), large.reservations().getFirst().amount());
            checks.isTrue("Exact large decimal passes", new ReservationEffectContract(46, new BigDecimal("9007199254740993.01"), "APPROVED").evaluate(large).passed());
            checks.isFalse("Distinct adjacent large decimal fails", new ReservationEffectContract(46, new BigDecimal("9007199254740992.01"), "APPROVED").evaluate(large).passed());
            checks.isFalse("Database IDs are invocation-local", db.id().equals(other.id()));
            checks.equals("Independent database has no leaked rows", 0, other.reader(evidence).read(42).reservations().size());
            if (System.getProperty("atf.eventuate.sql.probe", "none").equals("duplicate"))
                checks.equals("Controlled duplicate effect mismatch", 1, duplicate.reservations().size());
        }
    }
}
