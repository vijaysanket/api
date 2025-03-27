package com.socialeazy.api.services;

import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.ContentEntity;
import com.socialeazy.api.entities.MediaEntity;
import com.socialeazy.api.entities.PostsEntity;

import java.util.List;
import java.util.Map;

public interface Connector {
    String getName();

    String getAuthUrl();

    void handleAuthRedirect(Map<String, String> requestBody);

    void post(AccountsEntity accountEntity, PostsEntity postsEntity, List<MediaEntity> mediaEntity,ContentEntity contentEntity, boolean retry);

    void post(AccountsEntity accountEntity, PostsEntity postsEntity, boolean retry);
}
