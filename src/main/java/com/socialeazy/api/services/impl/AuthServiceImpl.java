package com.socialeazy.api.services.impl;

import com.socialeazy.api.domains.requests.SigninRequest;
import com.socialeazy.api.domains.requests.SignupRequest;
import com.socialeazy.api.domains.responses.AuthResponse;
import com.socialeazy.api.entities.OrganisationEntity;
import com.socialeazy.api.entities.UserEntity;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.OrganisationRepo;
import com.socialeazy.api.repo.UserRepo;
import com.socialeazy.api.security.TokenProvider;
import com.socialeazy.api.services.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private OrganisationRepo organisationRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private TokenProvider tokenProvider;

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest signupRequest) {
        OrganisationEntity organisationEntity = new OrganisationEntity();
        organisationEntity.setName(signupRequest.getName()+"'s org");
        organisationEntity.setAddedAt(LocalDateTime.now());
        organisationEntity = organisationRepo.save(organisationEntity);

        UserEntity userEntity = new UserEntity();
        userEntity.setName(signupRequest.getName());
        userEntity.setPassword(signupRequest.getPassword());
        userEntity.setActive(true);
        userEntity.setAddedAt(LocalDateTime.now());
        userEntity.setOrgId(organisationEntity.getId());
        userEntity.setEmailId(signupRequest.getEmailId());
        userEntity = userRepo.save(userEntity);

        String token = tokenProvider.createToken(userEntity);
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(userEntity.getName());
        response.setOrgId(userEntity.getOrgId());
        response.setUserId(userEntity.getId());
        return response;
    }

    @Override
    public AuthResponse signin(SigninRequest signinRequest) {
        Optional<UserEntity> userEntityOptional = userRepo.findByEmailIdAndPasswordAndIsActive(signinRequest.getEmailId(), signinRequest.getPassword(), true);
        if(userEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Incorrect UserId/Password");
        }
        UserEntity userEntity = userEntityOptional.get();
        String token = tokenProvider.createToken(userEntity);
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setName(userEntity.getName());
        response.setOrgId(userEntity.getOrgId());
        response.setUserId(userEntity.getId());
        return response;
    }
}


//Hey now onwards lets start with auth.  kindly remove the mock value you are setting in localstorage at authToken.  On load the localstorage should be empty.  if authtoken is not there then redirect the user to sign-in page