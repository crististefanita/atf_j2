package atf.eventuate.await;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;

/** Stackless evidence attached to, rather than substituted for, the original failure. */
public final class OrderConvergenceFailure extends RuntimeException {
    private final OrderConvergenceSnapshot snapshot;
    private OrderConvergenceFailure(OrderConvergenceSnapshot snapshot) {
        super("Order convergence evidence: " + snapshot.termination(), null, false, false);
        this.snapshot = snapshot;
    }
    static void attach(Throwable original, OrderConvergenceSnapshot snapshot) {
        original.addSuppressed(new OrderConvergenceFailure(snapshot));
    }
    public OrderConvergenceSnapshot snapshot() { return snapshot; }
    public static Optional<OrderConvergenceSnapshot> find(Throwable failure) {
        var pending = new ArrayDeque<Throwable>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<Throwable, Boolean>());
        if (failure != null) pending.add(failure);
        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            if (!seen.add(current)) continue;
            if (current instanceof OrderConvergenceFailure evidence) return Optional.of(evidence.snapshot);
            var suppressed = current.getSuppressed();
            for (int i = suppressed.length - 1; i >= 0; i--) pending.addLast(suppressed[i]);
            if (current.getCause() != null) pending.addLast(current.getCause());
        }
        return Optional.empty();
    }
}
