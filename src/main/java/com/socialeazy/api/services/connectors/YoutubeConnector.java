package com.socialeazy.api.services.connectors;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.*;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import io.jsonwebtoken.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.awt.desktop.SystemEventListener;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

import static com.mysql.cj.util.StringUtils.isEmptyOrWhitespaceOnly;
import static com.mysql.cj.util.StringUtils.urlEncode;
import static org.apache.logging.log4j.ThreadContext.isEmpty;


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
    private AccountsRepo accountsRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();






    @Override
    public String getName() {
        return "youtube";
    }

    @Override
    public String getAuthUrl() {
            String state = generateRandomState();
            String authorizationUrl = buildAuthorizationUrl(clientId, redirectUri, scope, state);
            AuthAssetEntity authAssetEntity = new AuthAssetEntity();
            authAssetEntity.setState(state);
            authAssetRepo.save(authAssetEntity);
            System.out.println(authAssetEntity);
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
            System.out.println(userDetails.asText());



            String youtubeId = userDetails.get("items").get(0).get("id").asText();
            String name = userDetails.get("items").get("snippet").get("title").asText();
            String profileUrl = userDetails.get("items").get("snippet").get("thumbnails").get("default").get("url").asText();

            AccountsEntity accountsEntity = new AccountsEntity();
            accountsEntity.setAccountHandle("youtube");
            accountsEntity.setAccessToken(accessToken);
            accountsEntity.setConnectedAt(LocalDateTime.now());
            accountsEntity.setValidTill(LocalDateTime.now().plusSeconds(expiresIn));
            accountsEntity.setRefreshToken(refreshToken);
            accountsEntity.setUserId(1);
            accountsEntity.setProfilePicture(profileUrl);
            accountsEntity.setAccountName(name);
            accountsEntity.setChannelId(youtubeId);
            accountsEntity = accountsRepo.save(accountsEntity);



        } catch (IOException e){
            throw new RuntimeException(e);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity, ContentEntity contentEntity, boolean retry) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accountEntity.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Prepare video metadata
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> snippet = new HashMap<>();
        snippet.put("title", contentEntity.getText());
        snippet.put("description", "Uploaded via API");
        snippet.put("tags", List.of("sample", "video", "YouTube API"));
        snippet.put("categoryId", "22"); // Category ID for "People & Blogs"

        Map<String, Object> status = new HashMap<>();
        status.put("privacyStatus", "public");

        requestBody.put("snippet", snippet);
        requestBody.put("status", status);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            // Step 1: Initiate Upload (Retrieve Upload URL)
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet,status",
                    HttpMethod.POST, entity, Map.class);

            System.out.println("Response Code: " + response.getStatusCodeValue());

            if (response.getStatusCode().is2xxSuccessful()) {
                String uploadUrl = (String) response.getBody().get("uploadUrl");
                String videoId = (String) response.getBody().get("id");

                // Step 2: Upload Video File
                uploadVideoFile(uploadUrl, mediaEntity.get(0)); // Assume first media file is the video
                System.out.println("Uploaded Video ID: " + videoId);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            if (retry) {
                refreshAccessToken(accountEntity);
                post(accountEntity, postsEntity, mediaEntity, contentEntity, false);
            } else {
                throw new RuntimeException("YouTube API request failed.");
            }
        }
    }
    private void uploadVideoFile(String uploadUrl, MediaEntity mediaEntity) {
        File videoFile = new File(mediaEntity.getMediaUrl()); // Assuming file path is stored

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(videoFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity, String.class);
            System.out.println("Upload Response: " + response.getBody());
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Video upload failed: " + e.getResponseBodyAsString());
        }
    }





    //    @Override
//    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity, ContentEntity contentEntity, boolean retry) {
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type", "application/json");
//        headers.set("Authorization", "Bearer " + accountEntity.getAccessToken());
//
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("text", contentEntity.getText());
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        // Making POST request
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(BASE_URL + "/tweets", HttpMethod.POST, entity, String.class);
//            System.out.println("Response Code: " + response.getStatusCodeValue());
//            System.out.println("Response Body: " + response.getBody());
//        } catch(HttpClientErrorException.Unauthorized e) {
//            if(retry) {
//                refreshAccessToken(accountEntity);
//                post(accountEntity, postsEntity, mediaEntity,contentEntity,false);
//            } else {
//                throw new RuntimeException("Something went wrong");
//            }
//        }
//    }
    private JsonNode fetchYoutubeUserDetails(String accessToken) throws java.io.IOException, InterruptedException {
        String userInfoUrl = BASE_URL + "channels?part=snippet,statistics&mine=true&fields=items(id,snippet(title,description,thumbnails),statistics)";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoUrl))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
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
                "state=" + state + "&" +
                "prompt=consent";
    }

    private void refreshAccessToken(AccountsEntity accountEntity) {
        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("client_id", clientId);
        tokenData.put("client_secret", clientSecret);
        tokenData.put("refresh_token", accountEntity.getRefreshToken());
        tokenData.put("grant_type", "refresh_token");
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
            JsonNode jsonResponse = objectMapper.readTree(response.body());

            String accessToken = jsonResponse.get("access_token").asText();
            String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;
            accountEntity.setAccessToken(accessToken);
            accountEntity.setRefreshToken(refreshToken);
            accountsRepo.save(accountEntity);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }





}