package com.socialeazy.api.repo;

import com.socialeazy.api.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByEmailIdAndPasswordAndIsActive(String emailId, String password, boolean isActive);
}
