package com.quanttrade.executor.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrade.executor.client.HttpSignalClient;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.SignalChecksum;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

    @Test
    void v2ExampleSignalUsesStableCycleIdempotency() throws IOException {
        Path root = Path.of("..", "contracts", "signal", "v2", "examples", "etf_stock_daily_v2.json").normalize();
        String json = Files.readString(root);
        JsonNode signal = mapper.readTree(json);

        assertEquals("2.0.0", signal.get("schema_version").asText());
        assertEquals("2026-03-06", signal.get("trading_date").asText());
        assertEquals("v2", signal.get("strategy_version").asText());
        assertEquals("DAILY_CLOSE", signal.get("rebalance_cycle").asText());
        assertEquals("PUBLISHED", signal.get("status").asText());

        String expected = signal.get("strategy_id").asText()
            + "|" + signal.get("strategy_version").asText()
            + "|" + signal.get("account_id").asText()
            + "|" + signal.get("trading_date").asText()
            + "|" + signal.get("rebalance_cycle").asText();
        assertEquals(expected, signal.get("idempotency_key").asText());
        assertEquals(signal.get("checksum").asText(), SignalChecksum.compute(HttpSignalClient.parseSignal(signal)));
    }

    @Test
    void checksumCanonicalizesNestedMetadataLikePython() {
        Signal signal = new Signal(
            "2.0.0",
            "sig-metadata-fixture",
            "acct",
            LocalDate.parse("2026-03-06"),
            OffsetDateTime.parse("2026-03-06T15:30:00+08:00"),
            "etf_stock_daily",
            "v2",
            "DAILY_CLOSE",
            "csv-20260306-v1",
            "PUBLISHED",
            List.of("510300.SH"),
            0.2,
            List.of(new com.quanttrade.executor.domain.PortfolioTarget("510300.SH", 0.8, 0.7, "trend")),
            new com.quanttrade.executor.domain.SignalConstraints(
                0.2,
                0.9,
                0.01,
                0.08,
                "DAILY_CLOSE",
                100000000.0,
                List.of("ST")
            ),
            null,
            "etf_stock_daily|v2|acct|2026-03-06|DAILY_CLOSE",
            Map.of(
                "zeta", Map.of("rank", 1.0, "items", List.of(2.0, Map.of("b", 3.0, "a", 4.5))),
                "alpha", true
            )
        );

        assertEquals("sha256:2bef987f89a8d113a1a248de98d85d89b551cb995044c019042c6b68057d19ed", SignalChecksum.compute(signal));
    }
}
