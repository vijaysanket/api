package com.socialeazy.api.services;

import com.socialeazy.api.domains.requests.PostRequest;
import org.springframework.stereotype.Service;

@Service
public interface PostService {
    void createPost(int userId, int orgId, PostRequest postRequest);
}
