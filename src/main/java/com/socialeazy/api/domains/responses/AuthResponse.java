package com.socialeazy.api.domains.responses;

import lombok.Data;

@Data
public class AuthResponse {
    private String name;

    private String token;

    private String profilePicture;

    private int orgId;

    private int userId;
}
