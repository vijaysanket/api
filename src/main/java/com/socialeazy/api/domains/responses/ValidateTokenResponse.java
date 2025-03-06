package com.socialeazy.api.domains.responses;

import lombok.Data;

@Data
public class ValidateTokenResponse {
    private int userId;

    private int orgId;

}
