package com.socialeazy.api.controller;

import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.domains.responses.PostResponse;
import com.socialeazy.api.services.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @PutMapping("/post")
    public PostResponse updatePost(@RequestHeader int userId, @RequestHeader int orgId, @RequestBody PostRequest postRequest, @RequestParam int postId) {
        return postService.updatePost(userId, orgId, postRequest, postId);
    }

    @GetMapping("/posts")
    public List<PostResponse> getPosts(@RequestParam LocalDate fromDate, @RequestParam LocalDate toDate, @RequestHeader int userId, @RequestHeader int orgId) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime fromDateTime = LocalDateTime.parse(fromDate + " 00:00:00", formatter);
        LocalDateTime toDateTime = LocalDateTime.parse(toDate + " 23:59:59", formatter);

        return postService.getPosts(userId, orgId, fromDateTime, toDateTime);
    }

}
