package com.socialeazy.api.services.impl;

import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.domains.responses.PostResponse;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.PostAccountsEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.exceptions.ResourceNotFoundException;
import com.socialeazy.api.mappers.AccountsMapper;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.PostAccountsRepo;
import com.socialeazy.api.repo.PostsRepo;
import com.socialeazy.api.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostsRepo postsRepo;

    @Autowired
    private PostAccountsRepo postAccountsRepo;

    @Autowired
    private AccountsMapper accountsMapper;

    @Autowired
    private AccountsRepo accountsRepo;

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

    @Override
    public List<PostResponse> getPosts(int userId, int orgId, LocalDateTime fromDate, LocalDateTime toDate) {
        List<PostsEntity> scheduledPosts = postsRepo.findByScheduledAtBetween(fromDate, toDate);
        List<PostResponse> responseList = new ArrayList<>();
        for(PostsEntity postEntity : scheduledPosts) {
            List<PostAccountsEntity> postAccountsEntityList = postAccountsRepo.findByPostId(postEntity.getId());
            List<Integer> accountIdsList = postAccountsEntityList.stream().map(postAccountsEntity -> {
                return postAccountsEntity.getAccountId();
            }).toList();
            PostResponse postResponse = new PostResponse();
            postResponse.setPostText(postEntity.getPostText());
            postResponse.setId(postEntity.getId());
            postResponse.setScheduledAt(postEntity.getScheduledAt());
            postResponse.setStatus(postEntity.getStatus());

            List<AccountsEntity> accountsEntityList = accountsRepo.findAllById(accountIdsList);

            postResponse.setAccounts(accountsMapper.mapToResponseList(accountsEntityList));
            responseList.add(postResponse);
        }
        return responseList;
    }

    @Override
    public PostResponse updatePost(int userId, int orgId, PostRequest postRequest, int postId) {
        Optional<PostsEntity> existingPostOptional = postsRepo.findById(postId);
        if(!existingPostOptional.isPresent()) {
            throw new ResourceNotFoundException("Post corressponding to postId not exists");
        }

        return null;
    }
}
