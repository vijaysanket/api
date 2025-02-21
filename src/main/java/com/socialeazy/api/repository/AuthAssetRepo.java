package com.socialeazy.api.repository;

import com.socialeazy.api.entity.AuthAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthAssetRepo extends JpaRepository<AuthAsset,String> {
}
