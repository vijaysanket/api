package com.socialeazy.api.services.connectors;


import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialeazy.api.entities.*;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
//    @Override
//    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {
//
//        if (accountEntity == null || accountEntity.getAccessToken() == null) {
//            throw new RuntimeException("Invalid account entity or missing access token");
//        }
//        // Facebook API URL for posting
//        String pageId = accountEntity.getChannelId();  // The Page ID
//        String postUrl = BASE_URL  + pageId + "/photos";
//
//        // Creating request headers
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type", "application/json");
//
//        // Creating request body
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("message", postsEntity.getPostText());
//        requestBody.put("access_token", accountEntity.getAccessToken());  // Page Access Token
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        // Making POST request to Facebook Graph API
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, entity, String.class);
//            System.out.println("Response Code: " + response.getStatusCodeValue());
//            System.out.println("Response Body: " + response.getBody());
//        } catch (HttpClientErrorException e) {
//            System.err.println("Facebook API Error: " + e.getMessage());
//            throw new RuntimeException("Failed to post on Facebook: " + e.getMessage());
//        }
//    }
//    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity, boolean retry) {
//
//        if (accountEntity == null || accountEntity.getAccessToken() == null) {
//            throw new RuntimeException("Invalid account entity or missing access token");
//        }
//        String pageId = accountEntity.getChannelId();
//        String postUrl;
//
//        // Conditional endpoint selection
//        if (mediaEntity.getMedia_url() != null) {
//            if (mediaEntity.getMedia_type() == "video") {
//                postUrl = BASE_URL + pageId + "/videos";  // Video Post
//            } else {
//                postUrl = BASE_URL + pageId + "/photos";  // Image Post
//            }
//        } else {
//            postUrl = BASE_URL + pageId + "/feed";         // Text-only Post
//        }
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);  // Modified content type for media upload
//
//        // Creating request body
//        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
//        requestBody.add("message", postsEntity.getPostText());
//        requestBody.add("access_token", accountEntity.getAccessToken());
//
//        // Adding media if available
//        if (mediaEntity.getMedia_url() != null) {
//            if (mediaEntity.getMedia_type() == "video") {
//                requestBody.add("file_url", mediaEntity.getMedia_url());  // Video URL for remote upload
//            } else {
//                requestBody.add("url", mediaEntity.getMedia_url());       // Image URL for remote upload
//            }
//        }
//
//        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, entity, String.class);
//            System.out.println("Response Code: " + response.getStatusCodeValue());
//            System.out.println("Response Body: " + response.getBody());
//        } catch (HttpClientErrorException e) {
//            System.err.println("Facebook API Error: " + e.getMessage());
//            throw new RuntimeException("Failed to post on Facebook: " + e.getMessage());
//        }
//    }

    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntities, ContentEntity contentEntity, boolean retry) {
        if (accountEntity == null || accountEntity.getAccessToken() == null) {
            throw new RuntimeException("Invalid account entity or missing access token");
        }

        String pageId = accountEntity.getChannelId();
        String postType = contentEntity.getPostType().toLowerCase(); // "video", "image", or "text"

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("access_token", accountEntity.getAccessToken());

        if ("video".equals(postType)) {
            // **🔹 Video Post Handling**
            Optional<MediaEntity> videoEntity = mediaEntities.stream()
                    .filter(m -> "video".equalsIgnoreCase(m.getMediaType()))
                    .findFirst();

            if (videoEntity.isPresent()) {
                requestBody.add("file_url", videoEntity.get().getMediaUrl());
                requestBody.add("description", contentEntity.getText()); // Text (Caption) for Video

                String postUrl = BASE_URL + pageId + "/videos"; // Facebook API for video post
                HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                postToFacebook(postUrl, entity);
            } else {
                throw new RuntimeException("No valid video found for posting.");
            }

        } else if ("image".equals(postType)) {
            // **🔹 Multiple Image Post Handling**
            List<String> imageUrls = mediaEntities.stream()
                    .filter(m -> "image".equalsIgnoreCase(m.getMediaType()))
                    .map(MediaEntity::getMediaUrl)
                    .collect(Collectors.toList());

            if (!imageUrls.isEmpty()) {
                List<String> photoIds = new ArrayList<>();

                for (String imgUrl : imageUrls) {
                    MultiValueMap<String, Object> photoRequest = new LinkedMultiValueMap<>();
                    photoRequest.add("url", imgUrl);
                    photoRequest.add("userMessage",contentEntity.getText());
                    photoRequest.add("published", "false"); // Unpublished to be attached later
                    photoRequest.add("access_token", accountEntity.getAccessToken());

                    HttpEntity<MultiValueMap<String, Object>> photoEntity = new HttpEntity<>(photoRequest, headers);
                    ResponseEntity<String> response = postToFacebook(BASE_URL + pageId + "/photos", photoEntity);

                    if (response != null && response.getBody().contains("id")) {
                        String photoId = extractPhotoId(response.getBody());
                        photoIds.add(photoId);
                    }
                }

                // **Attach Images to a Single Feed Post**
                MultiValueMap<String, Object> feedRequest = new LinkedMultiValueMap<>();
                feedRequest.add("message", contentEntity.getText()); // Text at the end
                for (String photoId : photoIds) {
                    feedRequest.add("attached_media", "{\"media_fbid\":\"" + photoId + "\"}");
                }
                feedRequest.add("access_token", accountEntity.getAccessToken());

                HttpEntity<MultiValueMap<String, Object>> feedEntity = new HttpEntity<>(feedRequest, headers);
                postToFacebook(BASE_URL + pageId + "/feed", feedEntity);
            } else {
                throw new RuntimeException("No valid images found for posting.");
            }

        } else {
            // **🔹 Text-Only Post**
            requestBody.add("message", contentEntity.getText());

            String postUrl = BASE_URL + pageId + "/feed"; // Facebook API for text-only post
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            postToFacebook(postUrl, entity);
        }
    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry) {

    }


    // **Helper Method to Post to Facebook API**
    private ResponseEntity<String> postToFacebook(String url, HttpEntity<MultiValueMap<String, Object>> entity) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("Facebook Post Success: " + response.getBody());
            return response;
        } catch (HttpClientErrorException e) {
            System.err.println("Failed to post to Facebook: " + e.getMessage());
            return null;
        }
    }

    // **Helper Method to Extract Photo ID from Response**
    private String extractPhotoId(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            return jsonNode.has("id") ? jsonNode.get("id").asText() : null;
        } catch (Exception e) {
            System.err.println("Failed to extract photo ID: " + e.getMessage());
            return null;
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
