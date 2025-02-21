package com.socialeazy.api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public interface ChannelService {
    String getAuthUrl(String channelName);

    void handleAuthRedirection(Map<String, String> requestBody);
}
