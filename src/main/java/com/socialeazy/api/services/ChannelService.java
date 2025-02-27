package com.socialeazy.api.services;

import com.socialeazy.api.domains.responses.ConnectedAccountResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ChannelService {
    String getAuthUrl(String channelName);

    void handleAuthRedirection(Map<String, String> requestBody);

    ConnectedAccountResponse getConnectedAccounts(int userId, int orgId);
}
