package com.quanttrade.executor.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void exampleSignalContainsMandatoryFields() throws IOException {
        Path root = Path.of("..", "contracts", "signal", "examples", "etf_stock_daily_v1.json").normalize();
        String json = Files.readString(root);
        JsonNode signal = mapper.readTree(json);

        assertEquals("1.0.0", signal.get("schema_version").asText());
        assertNotNull(signal.get("as_of"));
        assertNotNull(signal.get("strategy_id"));
        assertNotNull(signal.get("portfolio"));
        assertNotNull(signal.get("constraints"));

        String idempotencyKey = signal.get("idempotency_key").asText();
        String expectedPrefix = signal.get("strategy_id").asText() + "|" + signal.get("as_of").asText() + "|";
        assertTrue(idempotencyKey.startsWith(expectedPrefix));
    }
}
