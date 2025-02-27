package com.socialeazy.api.domains.responses;

import lombok.Data;

import java.util.List;

@Data
public class ConnectedAccountResponse {
    private List<ConnectedAccountData> data;

    private int connectedChannels;
}
