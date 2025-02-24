package com.socialeazy.api.services.impl;

import com.socialeazy.api.services.ChannelService;
import com.socialeazy.api.services.Connector;
import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChannelServiceImpl implements ChannelService {

    private final Map<String, Connector> channels = new HashMap<>();

    @Autowired
    public ChannelServiceImpl(List<Connector> connectorList) {
        connectorList.forEach(connector -> channels.put(connector.getName(), connector));
        System.out.println(connectorList.size());
    }

    @Override
    public String getAuthUrl(String connectorName) {
        return channels.get(connectorName).getAuthUrl();
    }

    @Override
    public void handleAuthRedirection(Map<String, String> requestBody) {
        channels.get(requestBody.get("channel")).handleAuthRedirect(requestBody);
    }


}
