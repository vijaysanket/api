package com.socialeazy.api.security;

import com.socialeazy.api.entities.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenProvider {
    @Value("${app.auth.tokenSecret}")
    private String tokenSecret;

    @Value("${app.auth.tokenExpirationMsec}")
    private long tokenExpirationMsec;

    private String clientId;
    public String createToken(UserEntity userEntity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpirationMsec);
        String compact = Jwts.builder()
                .setSubject(
                        userEntity.getId()+"/" +
                                userEntity.getOrgId()
                                )
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, tokenSecret)
                .compact();
        return compact;
    }
}
