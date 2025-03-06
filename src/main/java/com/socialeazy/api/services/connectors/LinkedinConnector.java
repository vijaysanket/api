package com.socialeazy.api.services.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class LinkedinConnector implements Connector {
    @Value("${channel.linkedin.clientid}")
    private String clientId;

    @Value("${channel.linkedin.clientsecret}")
    private String clientSecret;

    @Value("${channel.linkedin.redirecturi}")
    private String redirectUri;

    @Value("${channel.linkedin.scope}")
    private String scope;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private RestTemplate restTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String PROFILE_URL = "https://api.linkedin.com/v2/userinfo";
    private static final String EMAIL_URL = "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))";
    private static final String POST_URL = "https://api.linkedin.com/v2/ugcPosts";

    @Override
    public String getName() {
        return "linkedin";
    }

    // Step 1: Generate Authorization URL
    @Override
    public String getAuthUrl() {
        String state = generateRandomState();
        String authorizationUrl = buildAuthorizationUrl(clientId, redirectUri, state, scope);

        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setState(state);
        authAssetEntity.setStatus("new");
        authAssetRepo.save(authAssetEntity);

        return authorizationUrl;
    }

    // Step 2: Handle OAuth Redirect & Token Exchange
    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        String state = requestBody.get("state");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(state);

        if (authAssetEntityOptional.isEmpty()) {
            throw new RuntimeException("LinkedIn :: Potential CSRF attack detected");
        }

        String code = requestBody.get("code");
        String accessToken = getAccessToken(code);

        // Fetch LinkedIn User Profile & Email
        JsonNode userProfile = fetchLinkedInUserDetails(accessToken);
        //JsonNode userEmail = fetchLinkedInEmail(accessToken);




//        // Extract User Information
        String linkedInId = userProfile.get("sub").asText();
        String name = userProfile.get("name").asText();
        String profilePicture = userProfile.path("picture").asText();
        //String email = userEmail.path("elements").get(0).path("handle~").get("emailAddress").asText();

        // Save to Database
        AccountsEntity accountsEntity = new AccountsEntity();
        accountsEntity.setAccountHandle("linkedin");
        accountsEntity.setAccessToken(accessToken);
        accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(3600)); // 1-hour validity
        accountsEntity.setConnectedAt(LocalDateTime.now());
        accountsEntity.setAccountName(name);
        accountsEntity.setProfilePicture(profilePicture);
        accountsEntity.setChannelId(linkedInId);
        //accountsEntity.setUserId();
        accountsRepo.save(accountsEntity);

        System.out.println("✅ Successfully saved LinkedIn authentication data!");
    }

    // Step 3: Post Content to LinkedIn
    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {
        String accessToken = accountEntity.getAccessToken();
        String author = "urn:li:person:" + accountEntity.getChannelId();

        Map<String, Object> payload = Map.of(
                "author", author,
                "lifecycleState", "PUBLISHED",
                "specificContent", Map.of(
                        "com.linkedin.ugc.ShareContent", Map.of(
                                "shareCommentary", Map.of("text", postsEntity.getPostText()),
                                "shareMediaCategory", "NONE"
                        )
                ),
                "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(POST_URL, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("✅ Post successful!");
        } else {
            throw new RuntimeException("❌ Failed to post on LinkedIn: " + response.getBody());
        }
    }

    // Helper Method: Get LinkedIn Access Token
    private String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("grant_type", "authorization_code");
        requestParams.add("code", code);
        requestParams.add("redirect_uri", redirectUri);
        requestParams.add("client_id", clientId);
        requestParams.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestParams, headers);
        ResponseEntity<String> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode jsonResponse = new ObjectMapper().readTree(response.getBody());
                System.out.println(response.getBody());
                return jsonResponse.get("access_token").asText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse LinkedIn token response", e);
            }
        } else {
            throw new RuntimeException("Failed to obtain LinkedIn token: " + response.getBody());
        }

    }

    // Helper Methods: Fetch LinkedIn Profile & Email
    private JsonNode fetchLinkedInUserDetails(String accessToken) {
        return fetchLinkedInData(PROFILE_URL, accessToken);
    }

//    private JsonNode fetchLinkedInEmail(String accessToken) {
//        return fetchLinkedInData(EMAIL_URL, accessToken);
//    }

    private JsonNode fetchLinkedInData(String url, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        System.out.println("🔍 LinkedIn API Request: " + url);
        System.out.println("🔍 Access Token: " + accessToken);
        System.out.println("🔍 Response: " + response.getBody());

        try {
            return new ObjectMapper().readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to parse LinkedIn data: " + response.getBody(), e);
        }
    }


    // Generate Authorization URL
    private static String buildAuthorizationUrl(String clientId, String redirectUri, String state, String scopes) {
        return AUTHORIZATION_URL + "?" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "state=" + state + "&" +
                "scope=" + scopes.replace(",", "%20");
    }

    // Generate Secure Random State
    private static String generateRandomState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
