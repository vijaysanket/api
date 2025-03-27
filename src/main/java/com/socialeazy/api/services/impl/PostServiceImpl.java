package com.socialeazy.api.services.impl;

import com.socialeazy.api.constants.RuntimeConstants;
import com.socialeazy.api.domains.requests.AccountRequest;
import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.domains.responses.PostResponse;
import com.socialeazy.api.entities.*;
import com.socialeazy.api.exceptions.ResourceNotFoundException;
import com.socialeazy.api.mappers.AccountsMapper;
import com.socialeazy.api.repo.*;
import com.socialeazy.api.services.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private RuntimeConstants runtimeConstants;

    @Autowired
    private PostsRepo postsRepo;

    @Autowired
    private PostAccountsRepo postAccountsRepo;

    @Autowired
    private AccountsMapper accountsMapper;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private S3Service  s3Service;

    @Autowired
    private MediaRepo mediaRepo;

    @Autowired
    private ContentRepo contentRepo;



    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;




    public static final String[] Valid_status = {"DRAFT" ,"PUBLISHED" , "SCHEDULED"};


    @Override
    public void createPost(int userId, int orgId, PostRequest postRequest, MultipartFile[] mediaFiles) {

        if (postRequest.getScheduledAt() != null && postRequest.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time cannot be in the past");
        }

        PostsEntity postsEntity = new PostsEntity();

        postsEntity.setAddedAt(LocalDateTime.now());
        postsEntity.setScheduledAt(postRequest.getScheduledAt());
        postsEntity.setStatus("SCHEDULED");
        postsEntity.setUserId(userId);
        postsEntity.setOrgId(orgId);
        postsEntity = postsRepo.save(postsEntity);
        // Save PostAccountsEntity for each account ID

        // Iterate through accountRequest map
        for (Map.Entry<Integer, AccountRequest> entry : postRequest.getAccountRequest().entrySet()) {
            Integer accountId = entry.getKey();
            AccountRequest accountRequest = entry.getValue();

            // Save PostAccountsEntity
            PostAccountsEntity postAccountsEntity = new PostAccountsEntity();
            postAccountsEntity.setPostId(postsEntity.getId());
            postAccountsEntity.setAccountId(accountId);
            postAccountsEntity.setStatus(postRequest.getStatus());
            postAccountsEntity = postAccountsRepo.save(postAccountsEntity);

            // Save ContentEntity
            ContentEntity contentEntity = new ContentEntity();
            contentEntity.setPostId(postsEntity.getId());
            contentEntity.setPaId(postAccountsEntity.getId());
            contentEntity.setContentType(accountRequest.getContentType());
            contentEntity.setPostType(accountRequest.getPostType()); // Added postType
            contentEntity.setText(accountRequest.getText()); // Added text

            contentEntity = contentRepo.save(contentEntity);

            // ✅ Step 5: Upload Media to S3 and Save MediaEntity
            if (mediaFiles != null && mediaFiles.length > 0) {
                for (MultipartFile file : mediaFiles) {
                    if (file.isEmpty()) continue;
                    try (InputStream fileStream = file.getInputStream()) {
                        // Upload file to S3
                        String mediaUrl = s3Service.uploadFile(bucketName, file.getOriginalFilename(), fileStream);

                        // ✅ Save MediaEntity
                        MediaEntity mediaEntity = new MediaEntity();
                        mediaEntity.setContentId(contentEntity.getId());
                        mediaEntity.setMediaType(contentEntity.getPostType());
                        mediaEntity.setMediaUrl(mediaUrl);
                        mediaEntity.setCreatedAt(LocalDateTime.now());
                        mediaRepo.save(mediaEntity);
                    } catch (IOException e) {
                        throw new RuntimeException("Error uploading file to S3", e);
                    }
                }


            }
//        for(int accountId : postRequest.getAccountIds()) {
//            PostAccountsEntity postAccountsEntity = new PostAccountsEntity();
//            postAccountsEntity.setAccountId(accountId);
//            postAccountsEntity.setPostId(postsEntity.getId());
//            postAccountsEntity.setStatus("SCHEDULED");

            if(postRequest.getStatus().equals("NOW")) {
                List<MediaEntity> mediaEntities = mediaRepo.findBycontentId(contentEntity.getId());
                AccountsEntity accountEntity = accountsRepo.findById(postAccountsEntity.getAccountId()).get();

                runtimeConstants.channels.get(accountEntity.getAccountOf().toLowerCase()).post(accountEntity, postsEntity,mediaEntities,contentEntity,true);
                postAccountsEntity.setStatus("PUBLISHED");
            }
            postAccountsRepo.save(postAccountsEntity);
        }
        if(postRequest.getStatus().equals("NOW")) {
            postsEntity.setStatus("PUBLISHED");
        }
        postsRepo.save(postsEntity);

        }

    @Override
    public List<PostResponse> getPosts(int userId, int orgId, LocalDateTime fromDate, LocalDateTime toDate) {
        return List.of();
    }

    @Override
    public PostResponse updatePost(int userId, int orgId, PostRequest postRequest, int postId) {
        return null;
    }

}

//    @Override
//    public List<PostResponse> getPosts(int userId, int orgId, LocalDateTime fromDate, LocalDateTime toDate) {
//        List<PostsEntity> scheduledPosts = postsRepo.findByOrgIdAndScheduledAtBetween(orgId,fromDate, toDate);
//        List<PostResponse> responseList = new ArrayList<>();
//        for(PostsEntity postEntity : scheduledPosts) {
//            List<PostAccountsEntity> postAccountsEntityList = postAccountsRepo.findByPostId(postEntity.getId());
//            List<Integer> accountIdsList = postAccountsEntityList.stream().map(postAccountsEntity -> {
//                return postAccountsEntity.getAccountId();
//            }).toList();
//            PostResponse postResponse = new PostResponse();
//           // postResponse.setPostText(contentEntity.getText());
//            postResponse.setId(postEntity.getId());
//            postResponse.setScheduledAt(postEntity.getScheduledAt());
//            postResponse.setStatus(postEntity.getStatus());
//
//            List<AccountsEntity> accountsEntityList = accountsRepo.findAllById(accountIdsList);
//
//            postResponse.setAccounts(accountsMapper.mapToResponseList(accountsEntityList));
//            responseList.add(postResponse);
//        }
//        return responseList;
//    }
//
//    @Override
//    public PostResponse updatePost(int userId, int orgId, PostRequest postRequest, int postId) {
//        Optional<PostsEntity> existingPostOptional = postsRepo.findById(postId);
//        if(!existingPostOptional.isPresent()) {
//            throw new ResourceNotFoundException("Post corressponding to postId not exists");
//        }
//        PostsEntity existingPosts = existingPostOptional.get();
//        if(existingPosts.getStatus().equals("PUBLISHED")) {
//            throw new UnsupportedOperationException("Post already published");
//        }
//
//
//        //existingPosts.setText(contentEntity.getText());
//        existingPosts.setStatus(postRequest.getStatus());
//        existingPosts.setStatus(postRequest.getStatus());
//        existingPosts.setScheduledAt(postRequest.getScheduledAt());
//        postsRepo.save(existingPosts);
//
//        for (PostAccountsEntity postAccountsEntity : postAccountsRepo.findByPostId(postId)) {
//            if(!postRequest.getAccountIds().contains(postAccountsEntity.getAccountId())) {
//                postAccountsEntity.setStatus("DELETED");
//                postAccountsRepo.save(postAccountsEntity);
//            }
//        }
//
//        for(int accountId: postRequest.getAccountIds()) {
//            Optional<PostAccountsEntity> postAccountsEntity = postAccountsRepo.findByAccountIdAndPostId(accountId, postId);
//            if(!postAccountsEntity.isPresent()) {
//                PostAccountsEntity newPostAccountsEntity = new PostAccountsEntity();
//                newPostAccountsEntity.setAccountId(accountId);
//                newPostAccountsEntity.setPostId(postId);
//                newPostAccountsEntity.setStatus("SCHEDULED");
//                postAccountsRepo.save(newPostAccountsEntity);
//            }
//        }
//        PostsEntity postsEntity = existingPostOptional.get(); // Use the existing entity
//
//        // Validate status before setting it
//        if (!Arrays.asList(Valid_status).contains(postRequest.getStatus())) {
//            throw new IllegalArgumentException("Invalid status provided");
//        }
//
//        // Validate scheduled time
//        if (postRequest.getScheduledAt() != null && postRequest.getScheduledAt().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("Scheduled time cannot be in the past");
//        }
//
//        // Update fields
//        //postsEntity.setPostText(postRequest.getPostText());
//        postsEntity.setStatus(postRequest.getStatus());
//        postsEntity.setAddedAt(LocalDateTime.now()); // Updated timestamp
//        postsEntity.setScheduledAt(postRequest.getScheduledAt());
//        postsEntity.setUserId(userId);
//        postsEntity.setOrgId(orgId);
//
//        // Save updated entity
//        postsEntity = postsRepo.save(postsEntity);
//
//        // Return response
//        return new PostResponse();
//    }
//
//}
