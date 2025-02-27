package com.socialeazy.api.services.impl;

import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.entities.PostAccountsEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.repo.PostAccountsRepo;
import com.socialeazy.api.repo.PostsRepo;
import com.socialeazy.api.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostsRepo postsRepo;

    @Autowired
    private PostAccountsRepo postAccountsRepo;

    @Override
    public void createPost(int userId, int orgId, PostRequest postRequest) {
        PostsEntity postsEntity = new PostsEntity();
        postsEntity.setPostText(postRequest.getPostText());
        postsEntity.setStatus(postRequest.getStatus());
        postsEntity.setAddedAt(LocalDateTime.now());
        postsEntity.setScheduledAt(postRequest.getScheduledAt());
        postsEntity.setUserId(userId);
        postsEntity.setOrgId(orgId);
        postsEntity = postsRepo.save(postsEntity);

        for(int accountId : postRequest.getAccountIds()) {
            PostAccountsEntity postAccountsEntity = new PostAccountsEntity();
            postAccountsEntity.setAccountId(accountId);
            postAccountsEntity.setPostId(postsEntity.getId());
            postAccountsRepo.save(postAccountsEntity);
        }
    }
}
