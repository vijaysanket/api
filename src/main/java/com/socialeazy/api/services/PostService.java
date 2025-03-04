package com.socialeazy.api.services;

import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.domains.responses.PostResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface PostService {
    void createPost(int userId, int orgId, PostRequest postRequest);

    List<PostResponse> getPosts(int userId, int orgId, LocalDateTime fromDate, LocalDateTime toDate);

    PostResponse updatePost(int userId, int orgId, PostRequest postRequest, int postId);
}
