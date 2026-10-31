package atf.mock;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Optional;

/** Complete, unambiguous JSON for mock fixtures and matching; parsed trees never escape this type. */
final class MockJsonDocument {
    private static final ObjectReader JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .build().reader();
    private final JsonNode value;

    private MockJsonDocument(JsonNode value) { this.value = value; }

    static Optional<MockJsonDocument> read(String body) {
        try { return Optional.of(parse(body)); }
        catch (IOException | IllegalArgumentException invalid) { return Optional.empty(); }
    }

    static void require(String body, String field) {
        try { parse(body); }
        catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException(field + " must be one complete JSON document", invalid);
        }
    }

    boolean sameValue(MockJsonDocument other) { return value.equals(other.value); }

    private static MockJsonDocument parse(String body) throws IOException {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("JSON document is empty");
        JsonNode value = JSON.readTree(body);
        if (value == null) throw new IllegalArgumentException("JSON document is empty");
        return new MockJsonDocument(value);
    }
}
