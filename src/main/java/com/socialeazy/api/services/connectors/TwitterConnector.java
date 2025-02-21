package com.socialeazy.api.services.connectors;

import com.socialeazy.api.entity.AuthAsset;
import com.socialeazy.api.repository.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Component
public class TwitterConnector implements Connector {
    @Value("${channel.twitter.clientid}")
    private String clientId;

    @Value("${channel.twitter.clientsecret}")
    private String clientSecret;

    @Value("${channel.twitter.redirecturi}")
    private String redirectUri;

    @Value("${channel.twitter.scope}")
    private String scope;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://twitter.com/i/oauth2/authorize";



    @Override
    public String getName() {
        return "twitter";
    }

    @Override
    public String getAuthUrl() {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        String state = generateRandomState();
        String authorizationUrl = buildAuthorizationUrl(clientId, redirectUri, scope, state, codeChallenge);
        System.out.println("Authorization URL: " + authorizationUrl);

        return authorizationUrl;
    }


    private  void  saveurl(String state,String codeChallenge,String codeVerifier){
        AuthAsset authAsset = new AuthAsset();
        authAsset.setState(state);
        authAsset.setCodeChallenge(codeChallenge);
        authAsset.setCodeVerifier(codeVerifier);
        authAsset.setStatus("NEW");
        authAssetRepo.save(authAsset);

    }



    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        //requestBody.
    }

    private static String buildAuthorizationUrl(String clientId, String redirectUri, String scopes, String state, String codeChallenge) {

        return AUTHORIZATION_URL + "?" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + scopes + "&" +
                "state=" + state + "&" +
                "code_challenge=" + codeChallenge + "&" +
                "code_challenge_method=S256";

    }

    private static String generateCodeVerifier() {
        byte[] randomBytes = new byte[43];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Something went wrong");
        }

    }

    private static String generateRandomState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            //TODO:: handle exception
            return "";
        }
    }


}
