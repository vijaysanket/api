package com.socialeazy.api.services;

import java.util.Map;

public interface Connector {
    String getName();

    String getAuthUrl();

    void handleAuthRedirect(Map<String, String> requestBody);
}
