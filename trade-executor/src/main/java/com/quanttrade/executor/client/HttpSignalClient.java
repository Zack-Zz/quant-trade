package com.quanttrade.executor.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanttrade.executor.domain.Signal;
import com.quanttrade.executor.domain.PortfolioTarget;
import com.quanttrade.executor.domain.SignalConstraints;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpSignalClient implements SignalClient {
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpSignalClient(String endpoint) {
        this(endpoint, HttpClient.newHttpClient(), new ObjectMapper());
    }

    public HttpSignalClient(String endpoint, HttpClient httpClient, ObjectMapper objectMapper) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Signal fetchLatest(String accountId) {
        String encodedAccount = URLEncoder.encode(accountId, StandardCharsets.UTF_8);
        URI uri = URI.create(endpoint.replaceAll("/$", "") + "/api/v1/signals/latest?account_id=" + encodedAccount);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("signal service returned HTTP " + response.statusCode());
            }
            JsonNode envelope = objectMapper.readTree(response.body());
            if (!envelope.path("success").asBoolean(false)) {
                throw new IllegalStateException("signal service returned an unsuccessful envelope");
            }
            return parseSignal(envelope.path("data"));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to parse signal service response", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while fetching latest signal", ex);
        }
    }

    public static Signal parseSignal(JsonNode node) {
        validateAllowedFields(
            node,
            Set.of(
                "schema_version",
                "signal_id",
                "account_id",
                "trading_date",
                "as_of",
                "strategy_id",
                "strategy_version",
                "rebalance_cycle",
                "data_version",
                "status",
                "universe",
                "portfolio",
                "constraints",
                "checksum",
                "idempotency_key",
                "metadata"
            ),
            "signal"
        );
        JsonNode portfolio = node.path("portfolio");
        validateAllowedFields(portfolio, Set.of("cash_target_pct", "targets"), "portfolio");
        List<PortfolioTarget> targets = new ArrayList<>();
        for (JsonNode target : portfolio.path("targets")) {
            validateAllowedFields(target, Set.of("symbol", "target_pct", "confidence", "reason"), "portfolio.targets[]");
            targets.add(new PortfolioTarget(
                target.path("symbol").asText(),
                target.path("target_pct").asDouble(),
                target.hasNonNull("confidence") ? target.path("confidence").asDouble() : null,
                target.hasNonNull("reason") ? target.path("reason").asText() : null
            ));
        }

        JsonNode constraints = node.path("constraints");
        validateAllowedFields(
            constraints,
            Set.of(
                "max_turnover_pct",
                "max_single_position_pct",
                "max_daily_loss_pct",
                "max_drawdown_pct",
                "rebalance",
                "min_avg_daily_amount",
                "exclude_tags"
            ),
            "constraints"
        );
        SignalConstraints signalConstraints = new SignalConstraints(
            constraints.path("max_turnover_pct").asDouble(),
            constraints.path("max_single_position_pct").asDouble(),
            constraints.path("max_daily_loss_pct").asDouble(0.01),
            constraints.path("max_drawdown_pct").asDouble(0.08),
            constraints.path("rebalance").asText(),
            constraints.hasNonNull("min_avg_daily_amount") ? constraints.path("min_avg_daily_amount").asDouble() : null,
            parseStringList(constraints.path("exclude_tags"))
        );

        // Signal v2 embeds portfolio fields under `portfolio`, while the Java
        // domain record keeps execution-facing values flattened for planner use.
        return new Signal(
            node.path("schema_version").asText(),
            node.path("signal_id").asText(),
            node.path("account_id").asText(),
            LocalDate.parse(node.path("trading_date").asText()),
            OffsetDateTime.parse(node.path("as_of").asText()),
            node.path("strategy_id").asText(),
            node.path("strategy_version").asText(),
            node.path("rebalance_cycle").asText(),
            node.path("data_version").asText(),
            node.path("status").asText(),
            parseStringList(node.path("universe")),
            portfolio.path("cash_target_pct").asDouble(),
            targets,
            signalConstraints,
            node.hasNonNull("checksum") ? node.path("checksum").asText() : null,
            node.path("idempotency_key").asText(),
            parseObjectMap(node.path("metadata"))
        );
    }

    private static List<String> parseStringList(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            values.add(item.asText());
        }
        return values;
    }

    private static void validateAllowedFields(JsonNode node, Set<String> allowedFields, String path) {
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!allowedFields.contains(fieldName)) {
                throw new IllegalArgumentException("unknown " + path + " field: " + fieldName);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseObjectMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return Map.of();
        }
        return DEFAULT_MAPPER.convertValue(node, Map.class);
    }
}
