package com.socialeazy.api.domains.responses;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private int id;

    private String postText;

    private LocalDateTime scheduledAt;

    private String status;

    private List<ConnectedAccountData> accounts;
}
