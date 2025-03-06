package com.socialeazy.api.security;

import com.socialeazy.api.domains.responses.ValidateTokenResponse;
import com.socialeazy.api.entities.UserEntity;
import com.socialeazy.api.exceptions.InvalidTokenException;
import com.socialeazy.api.repo.UserRepo;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class TokenProvider {
    @Value("${app.auth.tokenSecret}")
    private String tokenSecret;

    @Value("${app.auth.tokenExpirationMsec}")
    private long tokenExpirationMsec;

    @Autowired
    private UserRepo userRepo;

    private String clientId;
    public String createToken(UserEntity userEntity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + tokenExpirationMsec);
        String compact = Jwts.builder()
                .setSubject(userEntity.getId()+"/" + userEntity.getOrgId())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, tokenSecret)
                .compact();
        return compact;
    }

    public ValidateTokenResponse validateToken(String authToken) {
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(tokenSecret).parseClaimsJws(authToken);
            System.out.println(claimsJws.getBody().get("sub"));
            String payload = (String) claimsJws.getBody().get("sub");
            String[] split = payload.split("/");
            System.out.println(split.length);
            ValidateTokenResponse validateTokenResponse = new ValidateTokenResponse();
            validateTokenResponse.setUserId(Integer.parseInt(split[0]));
            validateTokenResponse.setOrgId(Integer.parseInt(split[1]));
//            validateTokenResponse.setRole(Role.valueOf(split[2]));

            log.info("token filter userId::{} orgId::{}", Long.valueOf(split[0]),Long.valueOf(split[1]));
            Optional<UserEntity> byId = userRepo.findByIdAndOrgId(Long.valueOf(split[0]), Long.valueOf(split[1]));


            if(!byId.isPresent()) {
                throw new InvalidTokenException("User Doesn't exist in the system");
            }
            return validateTokenResponse;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature");
            throw new InvalidTokenException(ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
            throw new InvalidTokenException(ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
            throw new InvalidTokenException(ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
            throw new InvalidTokenException(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty.");
            throw new InvalidTokenException(ex.getMessage());
        } catch (Exception ex) {
            log.error("Uncaught exception :: "+ ex.getMessage());
            throw new InvalidTokenException(ex.getMessage());
        }
    }
}
