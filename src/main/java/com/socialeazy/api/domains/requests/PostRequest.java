package com.socialeazy.api.domains.requests;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostRequest {

    private String postText;

    private LocalDateTime scheduledAt;

    private String status = "Draft";

    private List<Integer> accountIds;
}
