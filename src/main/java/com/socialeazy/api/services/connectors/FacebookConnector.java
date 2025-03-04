package com.socialeazy.api.services.connectors;


import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.exceptions.UnAuthorizedException;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
    private RestTemplate restTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://www.facebook.com/v18.0/dialog/oauth";
    private static final String TOKEN_URL = "https://graph.facebook.com/v18.0/oauth/access_token";
    private static final String BASE_URL = "https://graph.facebook.com/v18.0";
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


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity) {

    }


    private static String bulidAuthorizationUrl(String clientId,String redirectUri, String scopes, String state){
        return AUTHORIZATION_URL + "?" +
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
