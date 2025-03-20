package com.socialeazy.api.services.impl;

import com.socialeazy.api.constants.RuntimeConstants;
import com.socialeazy.api.domains.requests.PostRequest;
import com.socialeazy.api.domains.responses.PostResponse;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.MediaEntity;
import com.socialeazy.api.entities.PostAccountsEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.exceptions.ResourceNotFoundException;
import com.socialeazy.api.mappers.AccountsMapper;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.MediaRepo;
import com.socialeazy.api.repo.PostAccountsRepo;
import com.socialeazy.api.repo.PostsRepo;
import com.socialeazy.api.services.PostService;
import com.socialeazy.api.services.impl.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


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


    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;


    public static final String[] Valid_status = {"DRAFT" ,"PUBLISHED" , "SCHEDULED"};


    @Override
    public void createPost(int userId, int orgId, PostRequest postRequest,MultipartFile[] mediaFiles) {

        if (postRequest.getScheduledAt() != null && postRequest.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time cannot be in the past");
        }

        PostsEntity postsEntity = new PostsEntity();
        postsEntity.setPostText(postRequest.getPostText());
        postsEntity.setAddedAt(LocalDateTime.now());
        postsEntity.setScheduledAt(postRequest.getScheduledAt());
        postsEntity.setStatus("SCHEDULED");
        postsEntity.setUserId(userId);
        postsEntity.setOrgId(orgId);
        postsEntity = postsRepo.save(postsEntity);

         //Upload Media to LocalStack S3 and Save URLs in DB
        if (mediaFiles != null && mediaFiles.length > 0) {
            for (MultipartFile file : mediaFiles) {
                System.out.println(file.getOriginalFilename());
                if (!file.isEmpty()) {
                    // List of supported video extensions
                    List<String> videoExtensions = Arrays.asList("mp4", "mov", "avi", "wmv", "flv", "mkv",
                            "webm", "3gp", "3g2", "m4v", "ts", "ogv",
                            "rm", "rmvb", "vob");
                    String fileName = file.getOriginalFilename();
                    String fileExtension = (fileName != null && fileName.contains("."))
                            ? fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase()
                            : "";
                    String mediaType = videoExtensions.contains(fileExtension) ? "video" : "image";


                    try (InputStream fileStream = file.getInputStream()) {
                            String mediaUrl = s3Service.uploadFile(bucketName, file.getOriginalFilename(), fileStream);  // Upload file to S3
                            System.out.println("Uploaded File Name: " + file.getOriginalFilename());

                            // Save Media URL in Media Table
                            MediaEntity mediaEntity = new MediaEntity();
                            mediaEntity.setPa_id(postsEntity.getId());
                            mediaEntity.setMedia_type("image");
                            mediaEntity.setMedia_url(mediaUrl);
                            mediaEntity.setCreated_at(LocalDateTime.now());
                            mediaRepo.save(mediaEntity);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException();
                        }
                }
            }
        }


        for(int accountId : postRequest.getAccountIds()) {
            PostAccountsEntity postAccountsEntity = new PostAccountsEntity();
            postAccountsEntity.setAccountId(accountId);
            postAccountsEntity.setPostId(postsEntity.getId());
            postAccountsEntity.setStatus("SCHEDULED");

            if(postRequest.getStatus().equals("NOW")) {
                AccountsEntity accountEntity = accountsRepo.findById(postAccountsEntity.getAccountId()).get();
                runtimeConstants.channels.get(accountEntity.getAccountOf().toLowerCase()).post(accountEntity, postsEntity, true);
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
        List<PostsEntity> scheduledPosts = postsRepo.findByOrgIdAndScheduledAtBetween(orgId,fromDate, toDate);
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
        PostsEntity existingPosts = existingPostOptional.get();
        if(existingPosts.getStatus().equals("PUBLISHED")) {
            throw new UnsupportedOperationException("Post already published");
        }


        existingPosts.setPostText(postRequest.getPostText());
        existingPosts.setStatus(postRequest.getStatus());
        existingPosts.setStatus(postRequest.getStatus());
        existingPosts.setScheduledAt(postRequest.getScheduledAt());
        postsRepo.save(existingPosts);

        for (PostAccountsEntity postAccountsEntity : postAccountsRepo.findByPostId(postId)) {
            if(!postRequest.getAccountIds().contains(postAccountsEntity.getAccountId())) {
                postAccountsEntity.setStatus("DELETED");
                postAccountsRepo.save(postAccountsEntity);
            }
        }

        for(int accountId: postRequest.getAccountIds()) {
            Optional<PostAccountsEntity> postAccountsEntity = postAccountsRepo.findByAccountIdAndPostId(accountId, postId);
            if(!postAccountsEntity.isPresent()) {
                PostAccountsEntity newPostAccountsEntity = new PostAccountsEntity();
                newPostAccountsEntity.setAccountId(accountId);
                newPostAccountsEntity.setPostId(postId);
                newPostAccountsEntity.setStatus("SCHEDULED");
                postAccountsRepo.save(newPostAccountsEntity);
            }
        }
        PostsEntity postsEntity = existingPostOptional.get(); // Use the existing entity

        // Validate status before setting it
        if (!Arrays.asList(Valid_status).contains(postRequest.getStatus())) {
            throw new IllegalArgumentException("Invalid status provided");
        }

        // Validate scheduled time
        if (postRequest.getScheduledAt() != null && postRequest.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time cannot be in the past");
        }

        // Update fields
        postsEntity.setPostText(postRequest.getPostText());
        postsEntity.setStatus(postRequest.getStatus());
        postsEntity.setAddedAt(LocalDateTime.now()); // Updated timestamp
        postsEntity.setScheduledAt(postRequest.getScheduledAt());
        postsEntity.setUserId(userId);
        postsEntity.setOrgId(orgId);

        // Save updated entity
        postsEntity = postsRepo.save(postsEntity);

        // Return response
        return new PostResponse();
    }

}
