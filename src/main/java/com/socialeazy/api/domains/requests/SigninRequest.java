package com.socialeazy.api.domains.requests;

import lombok.Data;

@Data
public class SigninRequest {
    private String emailId;

    private String password;
}
