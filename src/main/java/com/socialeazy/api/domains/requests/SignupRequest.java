package com.socialeazy.api.domains.requests;

import lombok.Data;

@Data
public class SignupRequest {
    private String name;

    private String emailId;

    private String password;
}
