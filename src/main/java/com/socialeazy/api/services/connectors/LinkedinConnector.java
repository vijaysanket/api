package com.socialeazy.api.services.connectors;

import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.repo.AuthAssetRepo;
import com.socialeazy.api.services.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

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
    private RestTemplate restTemplate;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String AUTHORIZATION_URL = "https://www.linkedin.com/oauth/v2/authorization?channel=linkedin";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Override
    public String getName() {
        return "linkedin";
    }

    @Override
    public String getAuthUrl(){
        String state = generateRandomState();
        String authurizationUrl = buildAuthorizationUrl(clientId,redirectUri,state,scope);

        AuthAssetEntity authAssetEntity = new AuthAssetEntity();
        authAssetEntity.setState(state);
        authAssetEntity.setStatus("new");
        authAssetRepo.save(authAssetEntity);
        return authurizationUrl;
    }

    @Override
    public void handleAuthRedirect(Map<String, String> requestBody) {

    }

    @Override
    public void post(AccountsEntity accountEntity, PostsEntity postsEntity) {

    }

    private static String buildAuthorizationUrl(String clientId, String redirectUri , String state, String scopes) {
        return AUTHORIZATION_URL + "&" +
                "response_type=code&" +
                "client_id=" + clientId + "&" +
                "redirect_uri=" + redirectUri + "&" +
                "state=" + state + "&" +
                "scope=" + scopes + "&" ;

    }
    static String generateRandomState() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }



}

