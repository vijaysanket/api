package com.socialeazy.api.services.connectors;


import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.mysql.cj.util.StringUtils.urlEncode;


@Component
public class FacebookConnector implements Connector {
    @Value("${channel.facebook.clientid}")
    private String clientId;

    @Value("${channel.facebook.clientsecret}")
    private String clientSecret;

    @Value("${channel.facebook.redirecturi}")
    private String redirectUri;

    @Value("${channel.facebook.scope}")
    private String scope;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    @Autowired
    private AccountsRepo accountsRepo;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://www.facebook.com/v18.0/dialog/oauth";
    private static final String TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";
    private static final String BASE_URL = "https://graph.facebook.com/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();


    @Override
    public String getName() {
        return "facebook";
    }

    public String getAuthUrl(){
        String state = generateRandomState();
        String authorizationUrl = bulidAuthorizationUrl(clientId,redirectUri ,scope, state);
        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setState(state);
        authAssetRepo.save(authAssetEntity);
        return  authorizationUrl;
    }

    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        System.out.println("Received requestBody: " + requestBody);
        String state = requestBody.get("state");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(state);
        if (authAssetEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Facebook :: Potential CSRF attack detected");
        }
        String authorizationCode = requestBody.get("code");
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("grant_type", "authorization_code");
        tokenData.put("code", authorizationCode);
        tokenData.put("redirect_uri", redirectUri);
        tokenData.put("client_id", clientId);
        tokenData.put("client_secret", clientSecret);
        tokenData.put("state",state);
        String formData = tokenData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");

        System.out.println("sending to facebook");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            String accessToken = jsonResponse.get("access_token").asText();
            //int expiresIn = jsonResponse.get("expires_in").asInt();
            JsonNode userDetails = (JsonNode)fetchFacebookUserDetails(accessToken);

            String facebookId = userDetails.get("id").asText();
            String username = userDetails.get("name").asText();
            String profileImageUrl = userDetails.get("picture").get("data").get("url").asText();

            //int followerCount = userDetails.get("data").get("public_metrics").get("followers_count").asInt();
            Optional<AccountsEntity> accountsEntityOptional = accountsRepo.findByAccountHandleAndUserId(username, 1);
            AccountsEntity accountsEntity = new AccountsEntity();
            if(accountsEntityOptional.isPresent()) {
                accountsEntity = accountsEntityOptional.get();
                accountsEntity.setAccessToken(accessToken);
                //accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));
            }
            else {
                accountsEntity.setAccountHandle(username);
                accountsEntity.setAccessToken(accessToken);
                //accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));
                accountsEntity.setConnectedAt(LocalDateTime.now());
                accountsEntity.setFollowerCount(0);
                accountsEntity.setAccountOf("Facebook");
                accountsEntity.setUserId(1);
                accountsEntity.setProfilePicture(profileImageUrl);
                accountsEntity.setAccountName(username);
                accountsEntity.setChannelId(facebookId);

            }
            accountsRepo.save(accountsEntity);
            System.out.println("Successfully saved Twitter authentication data!");

            // Step 3: Extract and save Facebook Page details
            if (userDetails.has("accounts")) {
                JsonNode pages = userDetails.get("accounts").get("data"); // Extracts pages list
                if (pages != null && pages.isArray()) {
                    for (JsonNode page : pages) {
                        String pageId = page.get("id").asText();
                        String pageAccessToken = page.get("access_token").asText();
                        String pageName = page.get("name").asText();

                        Optional<AccountsEntity> pageEntityOptional = accountsRepo.findByAccountHandleAndUserId(pageName, 1);
                        AccountsEntity pageEntity = pageEntityOptional.orElse(new AccountsEntity());

                        pageEntity.setAccountHandle(pageName);
                        pageEntity.setAccessToken(pageAccessToken);
                        //pageEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));
                        pageEntity.setConnectedAt(LocalDateTime.now());
                        pageEntity.setFollowerCount(0);
                        pageEntity.setAccountOf("Facebook Page");
                        pageEntity.setUserId(2);
                        pageEntity.setProfilePicture(profileImageUrl);
                        pageEntity.setAccountName(pageName);
                        pageEntity.setChannelId(pageId);

                        accountsRepo.save(pageEntity);
                        System.out.println("Saved Page: " + pageName + " with ID: " + pageId);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private TreeNode fetchFacebookUserDetails(String accessToken) throws IOException, InterruptedException {
        String userInfoUrl =  BASE_URL+"/me?fields=id,name,email,picture,accounts&access_token=" + accessToken;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response.body());
    }
    private TreeNode getFollowersCount(String accessToken){
        return null;
    }
    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {

        if (accountEntity == null || accountEntity.getAccessToken() == null) {
            throw new RuntimeException("Invalid account entity or missing access token");
        }
        // Facebook API URL for posting
        String pageId = accountEntity.getChannelId();  // The Page ID
        String postUrl = BASE_URL  + pageId + "/feed";

        // Creating request headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Creating request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", postsEntity.getPostText());
        requestBody.put("access_token", accountEntity.getAccessToken());  // Page Access Token

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Making POST request to Facebook Graph API
        try {
            ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, entity, String.class);
            System.out.println("Response Code: " + response.getStatusCodeValue());
            System.out.println("Response Body: " + response.getBody());
        } catch (HttpClientErrorException e) {
            System.err.println("Facebook API Error: " + e.getMessage());
            throw new RuntimeException("Failed to post on Facebook: " + e.getMessage());
        }
    }



    private static String bulidAuthorizationUrl(String clientId,String redirectUri, String scopes, String state){
        return AUTHORIZATION_URL + "?" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + urlEncode(scopes) + "&" +
                "state=" + state +
                "response_type=code&" ;
    }

    private static String generateRandomState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }


}
