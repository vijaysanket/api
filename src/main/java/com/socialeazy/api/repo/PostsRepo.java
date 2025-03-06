package com.socialeazy.api.repo;

import com.socialeazy.api.entities.PostsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PostsRepo  extends JpaRepository<PostsEntity, Integer> {
    List<PostsEntity> findByScheduledAtAndStatus(LocalDateTime scheduledAt, String status);

    @Query("SELECT m FROM PostsEntity m WHERE m.scheduledAt > :scheduledTime AND status = :status")
    List<PostsEntity> getScheduledPosts(LocalDateTime scheduledTime, String status);

    @Query("SELECT m FROM PostsEntity m WHERE m.scheduledAt BETWEEN :fromDate AND :endDate")
    List<PostsEntity> findByScheduledAtBetween(@Param("fromDate") LocalDateTime fromDate,
                                               @Param("endDate") LocalDateTime endDate);



    @Query("SELECT count(*) FROM PostsEntity m WHERE m.scheduledAt BETWEEN :fromDate AND :endDate AND orgId = :orgId AND status = :status")
    int findCountByOrgIdAndStatusAndScheduledBetween(int orgId, String status, LocalDateTime fromDate, LocalDateTime endDate);

    @Query(value = "SELECT p.* FROM Post p join User u where p.userId = u.id AND u.orgId = :orgId AND p.scheduledAt BETWEEN :fromDate AND :endDate", nativeQuery = true)
    List<PostsEntity> findByOrgIdAndScheduledAtBetween(int orgId, LocalDateTime fromDate, LocalDateTime endDate);
}
