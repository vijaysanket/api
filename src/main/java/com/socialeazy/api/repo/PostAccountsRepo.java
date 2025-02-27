package com.socialeazy.api.repo;

import com.socialeazy.api.entities.PostAccountsEntity;
import com.socialeazy.api.entities.PostsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostAccountsRepo extends JpaRepository<PostAccountsEntity, Integer> {
    List<PostAccountsEntity> findByPostId(int postId);
}
