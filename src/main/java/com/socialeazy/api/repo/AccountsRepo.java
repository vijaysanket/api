package com.socialeazy.api.repo;

import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountsRepo extends JpaRepository<AccountsEntity, Integer> {

    List<AccountsEntity> findByUserId(int userId);
}
