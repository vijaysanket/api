package com.socialeazy.api.repo;

import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.AuthAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountsRepo extends JpaRepository<AccountsEntity, Integer> {

    List<AccountsEntity> findByUserId(int userId);

    Optional<AccountsEntity> findByAccountHandle(String username);

    Optional<AccountsEntity> findByAccountHandleAndUserId(String username, int userId);

    @Query(value = "select sum(followerCount) followersCount from User u join Accounts a join (select distinct orgId from User u join Accounts a where u.id=a.userId and u.id =:userId) t where u.orgId=t.orgId", nativeQuery = true)
    Integer findCountByOrgIdAndActive(int userId, boolean active);
}
