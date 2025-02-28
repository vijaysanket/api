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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
import java.time.LocalDateTime;
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
    private AccountsRepo accountsRepo;






    @Autowired
    private RestTemplate restTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://twitter.com/i/oauth2/authorize";
    private static final String TOKEN_URL = "https://api.twitter.com/2/oauth2/token";
    private static final String BASE_URL = "https://api.twitter.com/2";
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
        authAssetEntity.setCodeChallenge(codeChallenge);
        //authAssetEntity.setStatus("now");
        authAssetRepo.save(authAssetEntity);
        System.out.println(authAssetEntity);
        System.out.println(codeChallenge);
        System.out.println(codeVerifier);
        return authorizationUrl;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            // Handle exception
            return "";
        }
    }


    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        String state = requestBody.get("state");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(state);
        if (authAssetEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Twitter :: Potential CSRF attack detected");
        }
        // Decode the authorization code
        String code = urlDecode(requestBody.get("code"));
        String codeVerifier = authAssetEntityOptional.get().getCodeVerifier();
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("grant_type", "authorization_code");
        tokenData.put("code", code);
        tokenData.put("redirect_uri", redirectUri);
        tokenData.put("client_id", clientId);
        tokenData.put("code_verifier", codeVerifier);
        String formData = tokenData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedCredentials)
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());

            // Parse JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonResponse = objectMapper.readTree("{\"token_type\":\"bearer\",\"expires_in\":7200,\"access_token\":\"aWNET29CNTFVWlZKM2ZEUUFUdUIwT2ZxMldVYnB5TWxQVE1XRDUzOE1uSWg0OjE3NDA3MjQzMDE0NzM6MToxOmF0OjE\",\"scope\":\"tweet.write users.read tweet.read offline.access\",\"refresh_token\":\"d09sdjJjQU5wUGJhNVRHYjU2U29pNGwxczUtdFM3eVpaU3NUOTZDb0tMUFRIOjE3NDA3MjQzMDE0NzM6MTowOnJ0OjE\"}");

            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            int expiresIn = jsonResponse.get("expires_in").asInt();

            JsonNode userDetails = fetchTwitterUserDetails(accessToken);


            // Extract user details
            String twitterId = userDetails.get("data").get("id").asText();
            String username = userDetails.get("data").get("username").asText();
            String profileImageUrl = userDetails.get("data").get("profile_image_url").asText();
            String accountName = userDetails.get("data").get("name").asText();
            int followerCount = userDetails.get("data").get("public_metrics").get("followers_count").asInt();
            Optional<AccountsEntity> accountsEntityOptional = accountsRepo.findByAccountHandleAndUserId(username, 1);
            AccountsEntity accountsEntity = new AccountsEntity();
            if(accountsEntityOptional.isPresent()) {
                accountsEntity = accountsEntityOptional.get();
                accountsEntity.setAccessToken(accessToken);
                accountsEntity.setRefreshToken(refreshToken);
                accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));

            } else {
                accountsEntity.setAccountHandle(username);
                accountsEntity.setAccessToken(accessToken);
                accountsEntity.setRefreshToken(refreshToken);
                accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));
                accountsEntity.setConnectedAt(LocalDateTime.now());
                accountsEntity.setFollowerCount(followerCount);
                accountsEntity.setAccountOf("TWITTER");
                accountsEntity.setUserId(1);
                accountsEntity.setProfilePicture(profileImageUrl);
                accountsEntity.setAccountName(accountName);
                accountsEntity.setChannelId(twitterId);
            }
            accountsRepo.save(accountsEntity);
            System.out.println("Successfully saved Twitter authentication data!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    private JsonNode fetchTwitterUserDetails(String accessToken) throws IOException, InterruptedException {
        String userInfoUrl = BASE_URL + "/users/me?user.fields=id,name,username,profile_image_url,public_metrics";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(response.body());
    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + accountEntity.getAccessToken());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("text", postsEntity.getPostText());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // Making POST request
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL+"/tweets", HttpMethod.POST, entity, String.class);

        // Printing response
        System.out.println("Response Code: " + response.getStatusCodeValue());
        System.out.println("Response Body: " + response.getBody());
    }

    private static String buildAuthorizationUrl(String clientId, String redirectUri, String scopes, String state, String codeChallenge) {

        return AUTHORIZATION_URL + "?" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + urlEncode(scopes) + "&" +
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
