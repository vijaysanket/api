package com.socialeazy.api.controller;

import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.services.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("v1")
public class PostController {
    @Autowired
    private PostService postService;

    @PostMapping("/post")
    public void createPost(@RequestHeader int userId, @RequestHeader int orgId, @RequestBody PostRequest postRequest) {
        postService.createPost(userId, orgId, postRequest);
    }

}
