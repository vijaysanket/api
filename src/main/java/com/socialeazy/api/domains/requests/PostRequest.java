package com.socialeazy.api.domains.requests;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PostRequest {


    private LocalDateTime scheduledAt;

    private String status = "Draft";

    private List<Integer> accountIds;

    private Map<Integer ,AccountRequest> accountRequest;

}
