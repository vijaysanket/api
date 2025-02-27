package com.socialeazy.api.repo;

import com.socialeazy.api.entities.PostsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PostsRepo  extends JpaRepository<PostsEntity, Integer> {
    List<PostsEntity> findByScheduledAt(LocalDateTime scheduledAt);
}
