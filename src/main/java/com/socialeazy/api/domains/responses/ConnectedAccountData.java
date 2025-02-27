package com.socialeazy.api.domains.responses;

import lombok.Data;

@Data
public class ConnectedAccountData {
    private String id;

    private String accountHandle;

    private String accountName;

    private String profilePicture;

    private int followerCount;

    private String channelName;

    //private String channelIcon;
}
