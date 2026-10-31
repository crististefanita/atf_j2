package atf.eventuate.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Wire validation precedes conversion, so out-of-range identities cannot alias valid orders. */
final class OrderResponseDecoder {
    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).build();
    private OrderResponseDecoder() { }

    static OrderObservation decode(String source) {
        try {
            var body = JSON.readTree(source);
            if (body == null || !body.isObject()) throw new IllegalArgumentException("Order response requires a JSON object");
            var id = body.path("orderId");
            if (!id.isIntegralNumber() || !id.canConvertToLong())
                throw new IllegalArgumentException("Order response orderId must be an integer in the signed long range");
            if (!body.path("orderState").isTextual())
                throw new IllegalArgumentException("Order response requires textual orderState");
            var reason = body.get("rejectionReason");
            if (reason == null || !(reason.isNull() || reason.isTextual()))
                throw new IllegalArgumentException("Order response requires nullable textual rejectionReason");
            return new OrderObservation(id.longValue(), body.path("orderState").textValue(), reason.isNull() ? null : reason.textValue());
        } catch (java.io.IOException failure) {
            throw new IllegalArgumentException("Invalid order response JSON", failure);
        }
    }
}
