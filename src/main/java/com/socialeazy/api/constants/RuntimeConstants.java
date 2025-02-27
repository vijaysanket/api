package com.socialeazy.api.constants;

import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuntimeConstants {
    public final Map<String, Connector> channels = new HashMap<>();

    @Autowired
    public RuntimeConstants(List<Connector> connectorList) {
        connectorList.forEach(connector -> channels.put(connector.getName(), connector));
        System.out.println(connectorList.size());
    }
}
