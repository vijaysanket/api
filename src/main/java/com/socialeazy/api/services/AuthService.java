package com.socialeazy.api.services;

import com.socialeazy.api.domains.requests.SigninRequest;
import com.socialeazy.api.domains.requests.SignupRequest;
import com.socialeazy.api.domains.responses.AuthResponse;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    AuthResponse signup(SignupRequest signupRequest);

    AuthResponse signin(SigninRequest signinRequest);
}
