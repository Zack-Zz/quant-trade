package com.quanttrade.executor.client;

import com.quanttrade.executor.domain.Signal;

public class HttpSignalClient implements SignalClient {
    private final String endpoint;

    public HttpSignalClient(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Signal fetchLatest(String accountId) {
        throw new UnsupportedOperationException(
            "HTTP integration is not wired yet. Endpoint=" + endpoint + ", accountId=" + accountId
        );
    }
}
