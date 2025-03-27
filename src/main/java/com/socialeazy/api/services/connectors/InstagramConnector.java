package com.socialeazy.api.services.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.entities.*;
import com.socialeazy.api.exceptions.UnAuthorizedException;

import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.security.SecureRandom;

import static com.socialeazy.api.services.connectors.LinkedinConnector.generateRandomState;

@Slf4j
@Component
public class InstagramConnector implements Connector {

    @Value("${channel.instagram.clientid}")
    private String clientId;

    @Value("${channel.instagram.clientsecret}")
    private String clientSecret;

    @Value("${channel.instagram.redirecturi}")
    private String redirectUri;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    @Autowired
    private AccountsRepo accountsRepo;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";
    private static final String BASE_URL = "https://graph.facebook.com/v18.0";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final String AUTHORIZATION_URL = "https://www.facebook.com/dialog/oauth";

    @Override
    public String getName() {
        return "instagram";
    }

    @Override
    public String getAuthUrl() {
        String state = generateRandomState();
        String authorizationUrl = AUTHORIZATION_URL +
                "?client_id=" + clientId +
                "&display=page" +
                "&extras=%7B%22setup%22%3A%7B%22channel%22%3A%22IG_API_ONBOARDING%22%7D%7D" +
                "&redirect_uri=" + redirectUri +
                "&response_type=code&scope=instagram_basic";

        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setState(state);
        authAssetRepo.save(authAssetEntity);
        log.info("Generated Instagram Auth URL: {}", authorizationUrl);
        return authorizationUrl;
    }

    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        String code = requestBody.get("code");
        if (code == null) {
            log.error("Authorization code is missing.");
            throw new RuntimeException("Authorization code is missing.");
        }

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
            JsonNode jsonResponse = new ObjectMapper().readTree(response.body());
            log.info("Instagram Token Response: {}", response.body());

            if (!jsonResponse.has("access_token")) {
                throw new RuntimeException("Access token not found.");
            }

            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            accessToken = exchangeForLongLivedToken(accessToken);
            String instagramBusinessId = fetchInstagramBusinessId(accessToken);

            AccountsEntity accountsEntity = new AccountsEntity();
            accountsEntity.setAccountHandle("instagram");
            accountsEntity.setAccessToken(accessToken);
            accountsEntity.setRefreshToken(refreshToken);
            accountsEntity.setConnectedAt(LocalDateTime.now());
            accountsEntity.setUserId(1);
            accountsEntity.setChannelId(instagramBusinessId);

            Map<String, String> profileDetails = fetchProfileDetails(instagramBusinessId, accessToken);
            accountsEntity.setAccountName(profileDetails.get("name"));
            accountsEntity.setProfilePicture(profileDetails.get("profile_picture"));
            accountsEntity.setFollowerCount(Integer.parseInt(profileDetails.get("followers")));

            accountsRepo.save(accountsEntity);
        } catch (IOException | InterruptedException e) {
            log.error("Error handling Instagram authentication: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity, ContentEntity contentEntity, boolean retry) {

    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {

    }

    private String exchangeForLongLivedToken(String shortLivedToken) throws IOException, InterruptedException {
        String url = BASE_URL + "/oauth/access_token?grant_type=fb_exchange_token&client_id=" + clientId + "&client_secret=" + clientSecret + "&fb_exchange_token=" + shortLivedToken;
        HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        JsonNode jsonResponse = new ObjectMapper().readTree(response.body());
        return jsonResponse.has("access_token") ? jsonResponse.get("access_token").asText() : shortLivedToken;
    }

    private String fetchInstagramBusinessId(String accessToken) throws IOException, InterruptedException {
        String url = BASE_URL + "/me?fields=accounts%7Bid,name,instagram_business_account%7D&access_token=" + accessToken;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))  // ✅ URL is now properly formatted
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode jsonResponse = new ObjectMapper().readTree(response.body());
        log.info("Instagram Business ID Response: {}", response.body());

        JsonNode accounts = jsonResponse.get("accounts");
        if (accounts != null && accounts.has("data") && accounts.get("data").size() > 0) {
            JsonNode firstPage = accounts.get("data").get(0);
            if (firstPage.has("instagram_business_account") && firstPage.get("instagram_business_account").has("id")) {
                return firstPage.get("instagram_business_account").get("id").asText();
            }
        }
        throw new RuntimeException("Instagram Business Account not found");
    }


    private Map<String, String> fetchProfileDetails(String businessId, String accessToken) throws IOException, InterruptedException {
        String url = BASE_URL + "/" + businessId + "?fields=name,profile_picture_url,followers_count&access_token=" + accessToken;
        HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
        JsonNode jsonResponse = new ObjectMapper().readTree(response.body());
        Map<String, String> details = new HashMap<>();
        details.put("name", jsonResponse.get("name").asText());
        details.put("profile_picture", jsonResponse.get("profile_picture_url").asText());
        details.put("followers", jsonResponse.get("followers_count").asText());
        return details;
    }

    private static String buildFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
    }
}
