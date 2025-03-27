package com.socialeazy.api.services.connectors;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.*;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import io.jsonwebtoken.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

import static com.mysql.cj.util.StringUtils.urlEncode;


@Component

public class YoutubeConnector implements Connector {

    @Value("${channel.youtube.clientid}")
    private String clientId;

    @Value("${channel.youtube.clientsecret}")
    private String clientSecret;

    @Value("${channel.youtube.redirecturi}")
    private String redirectUri;

    @Value("${channel.youtube.scope}")
    private String scope;

    @Autowired
    private AuthAssetRepo authAssetRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    //    private static final String INFO_URL = "https://www.googleapis.com/oauth2/v1/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();






    @Override
    public String getName() {
        return "youtube";
    }

    @Override
    public String getAuthUrl() {
            //String codeVerifier = generateCodeVerifier();
            //String codeChallenge = generateCodeChallenge(codeVerifier);
            String state = generateRandomState();
            String authorizationUrl = buildAuthorizationUrl(clientId, redirectUri, scope, state);
            AuthAssetEntity authAssetEntity = new AuthAssetEntity();
            //authAssetEntity.setCodeVerifier(codeVerifier);
            authAssetEntity.setState(state);
            //authAssetEntity.setCodeChallenge(codeChallenge);
            //authAssetEntity.setStatus("now");
            authAssetRepo.save(authAssetEntity);
            System.out.println(authAssetEntity);
            //System.out.println(codeChallenge);
            //System.out.println(codeVerifier);
            System.out.println(authorizationUrl);
            return authorizationUrl;

        }

    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {
        String state = requestBody.get("state");
        Optional<AuthAssetEntity> authAssetEntityOptional = authAssetRepo.findById(state);
        if (authAssetEntityOptional.isEmpty()) {
            throw new UnAuthorizedException("Youtube :: Potential CSRF attack detected");
        }

        String code = urlDecode(requestBody.get("code"));
        String codeVerifier = authAssetEntityOptional.get().getCodeVerifier();
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("client_id", clientId);
        tokenData.put("client_secret", clientSecret);
        tokenData.put("grant_type", "authorization_code");
        tokenData.put("redirect_uri", redirectUri);
        tokenData.put("code", code);
//        tokenData.put("code_verifier", codeVerifier);
        String formData = tokenData.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        try{
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            response.statusCode();
            System.out.println(response.body());

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            int expiresIn = jsonResponse.get("expires_in").asInt();
            JsonNode userDetails = fetchYoutubeUserDetails(accessToken);


            String twitterId = userDetails.get("data").get("id").asText();








        } catch (IOException e){
            throw new RuntimeException(e);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity, ContentEntity contentEntity, boolean retry) {

    }

    private JsonNode fetchYoutubeUserDetails(String accessToken) throws java.io.IOException, InterruptedException {
        String userInfoUrl = BASE_URL + "channels?part=snippet,statistics&mine=true&fields=items(id,snippet(title,description,thumbnails),statistics)";

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
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {

    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            // Handle exception
            return "";
        }
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
    private static String buildAuthorizationUrl(String clientId, String redirectUri, String scopes, String state) {
        return AUTHORIZATION_URL + "?" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "scope=" + urlEncode(scopes) + "&" +
                "access_type=offline" + "&" +
                "state=" + state + "&";
    }





}