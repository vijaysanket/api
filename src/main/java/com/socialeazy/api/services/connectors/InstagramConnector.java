package com.socialeazy.api.services.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.security.SecureRandom;

@Component
public class InstagramConnector implements Connector {
    @Value("${channel.instagram.clientid}")
    private String clientId;

    @Value("${channel.instagram.clientsecret}")
    private String clientSecret;

    @Value("${channel.instagram.redirecturi}")
    private String redirectUri;

    @Value("${channel.instagram.scope}")
    private String scope;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private RestTemplate restTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://api.instagram.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String BASE_URL = "https://graph.instagram.com";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "instagram";
    }

    @Override
    public String getAuthUrl() {
        String state = generateRandomState();
        String authorizationUrl = AUTHORIZATION_URL + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&scope=" + scope + "&response_type=code&state=" + state;

        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setState(state);
        authAssetRepo.save(authAssetEntity);

        return authorizationUrl;
    }

    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        String state = requestBody.get("state");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(state);
        if (authAssetEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Instagram :: Potential CSRF attack detected");
        }

        String code = requestBody.get("code");
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("client_id", clientId);
        tokenData.put("client_secret", clientSecret);
        tokenData.put("grant_type", "authorization_code");
        tokenData.put("redirect_uri", redirectUri);
        tokenData.put("code", code);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormData(tokenData)))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree(response.body());

            String accessToken = jsonResponse.get("access_token").asText();
            JsonNode userDetails = fetchInstagramUserDetails(accessToken);

            AccountsEntity accountsEntity = new AccountsEntity();
            accountsEntity.setAccountHandle("instagram");
            accountsEntity.setAccessToken(accessToken);
            accountsEntity.setConnectedAt(LocalDateTime.now());
            accountsEntity.setUserId(1);
            accountsEntity.setChannelId(userDetails.get("id").asText());
            accountsEntity.setAccountName(userDetails.get("username").asText());
            accountsRepo.save(accountsEntity);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity) {

    }

    private JsonNode fetchInstagramUserDetails(String accessToken) throws IOException, InterruptedException {
        String userInfoUrl = BASE_URL + "/me?fields=id,username&access_token=" + accessToken;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new ObjectMapper().readTree(response.body());
    }

    private static String buildFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }

    private static String generateRandomState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
