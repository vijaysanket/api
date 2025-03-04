package com.socialeazy.api.controller;

import com.socialeazy.api.domains.requests.SigninRequest;
import com.socialeazy.api.domains.requests.SignupRequest;
import com.socialeazy.api.domains.responses.AuthResponse;
import com.socialeazy.api.services.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("v1")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest signupRequest) {
        return authService.signup(signupRequest);
    }
    
    @PostMapping("signin")
    public AuthResponse signin(@RequestBody SigninRequest signinRequest) {
        return authService.signin(signinRequest);
    }

}
