package com.socialeazy.api.services.connectors;

import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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


    @Autowired
    private RestTemplate restTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://twitter.com/i/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.twitter.com/2/oauth2/token";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();



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
        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setCodeVerifier(codeVerifier);
        authAssetEntity.setState(state);
        authAssetEntity.setCodeChannelge(codeChallenge);
        authAssetRepo.save(authAssetEntity);
        return authorizationUrl;
    }


    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {

        String status = requestBody.get("status");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(status);
        if(authAssetEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Twitter :: Potential CSRF attack detected");
        }
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("grant_type", "authorization_code");
        tokenData.put("code", requestBody.get("code"));
        tokenData.put("redirect_uri", redirectUri);
        tokenData.put("client_id", clientId);
        tokenData.put("code_verifier", authAssetEntityOptional.get().getCodeVerifier());

        String formData = tokenData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + urlEncode(entry.getValue()))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
