package com.socialeazy.api.repo;

import com.socialeazy.api.entities.AuthAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthAssetRepo extends JpaRepository<AuthAssetEntity, String> {
}
