package atf.eventuate.tests.unit.live;

import atf.eventuate.live.LiveHttpClient;
import atf.eventuate.live.LiveSettings;
import atf.eventuate.evidence.EventuateEvidence;
import atf.core.http.response.ApiResponse;
import atf.testkit.AtfUnitTest;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** Pure transport doubles; these tests do not claim an actual LIVE network execution. */
@AtfUnitTest
class LiveHttpPolicyTest {
    private static LiveSettings settings() {
        return new LiveSettings(URI.create("http://orders.invalid"),URI.create("http://customers.invalid"),"unused:9092",
                "jdbc:mysql://database.invalid/test","reader","not-used",Duration.ofSeconds(2),Duration.ofSeconds(1));
    }
    @Test void mutationIsDispatchedOnceEvenAfterAnUnsuccessfulResponse() {
        var dispatches = new AtomicInteger(); var evidence = new EventuateEvidence();
        try(var client = new LiveHttpClient(settings(),evidence::record,timeout -> (request,url,auth)->{
            dispatches.incrementAndGet(); return CompletableFuture.completedFuture(new ApiResponse(500,"{}",Map.of(),Duration.ZERO,url));
        })) {
            assertThrows(IllegalStateException.class,()->client.createOrder(1,"12.34",Duration.ofSeconds(1)));
            assertThrows(IllegalStateException.class,()->client.createOrder(1,"12.34",Duration.ofSeconds(1)));
            assertEquals(1,dispatches.get()); assertEquals(1,evidence.exchanges().size());
        }
    }
    @Test void readReceivesOnlyRemainingTimeAndNeverRetriesA503() {
        var dispatches = new AtomicInteger(); var timeouts = new ArrayList<Duration>();
        try(var client = new LiveHttpClient(settings(),exchange->{},timeout -> {
            timeouts.add(timeout); return (request,url,auth)->{
                dispatches.incrementAndGet(); return CompletableFuture.completedFuture(new ApiResponse(503,"{}",Map.of(),Duration.ZERO,url));
            };
        })) {
            assertThrows(IllegalStateException.class,()->client.observe(42,Duration.ofMillis(750)));
            assertEquals(1,dispatches.get()); assertFalse(timeouts.isEmpty());
            assertTrue(timeouts.stream().allMatch(value->value.compareTo(Duration.ofMillis(750))<=0));
        }
    }
    @Test void validatesMutationIdentityWithoutRoundingOrReplaying() {
        var dispatches = new AtomicInteger();
        try(var client = new LiveHttpClient(settings(),exchange->{},timeout -> (request,url,auth)->{
            dispatches.incrementAndGet(); return CompletableFuture.completedFuture(new ApiResponse(201,"{\"orderId\":9223372036854775808}",Map.of(),Duration.ZERO,url));
        })) {
            assertThrows(IllegalStateException.class,()->client.createOrder(1,"12.34",Duration.ofSeconds(1)));
            assertThrows(IllegalStateException.class,()->client.createOrder(1,"12.34",Duration.ofSeconds(1)));
            assertEquals(1,dispatches.get());
        }
    }
}
