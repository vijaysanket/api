package com.socialeazy.api.workers;

import com.socialeazy.api.constants.RuntimeConstants;
import com.socialeazy.api.entities.AccountsEntity;
import com.socialeazy.api.entities.PostAccountsEntity;
import com.socialeazy.api.entities.PostsEntity;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.PostAccountsRepo;
import com.socialeazy.api.repo.PostsRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class PostScheduler {

    @Autowired
    private PostsRepo postsRepo;

    @Autowired
    private AccountsRepo accountsRepo;

    @Autowired
    private PostAccountsRepo postAccountsRepo;

    @Autowired
    private RuntimeConstants runtimeConstants;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    @SchedulerLock(name = "schedulePost", lockAtMostFor = "60s", lockAtLeastFor = "60s")
    public void schedulePost() {

        List<PostsEntity> postsEntityList = postsRepo.getScheduledPosts(LocalDateTime.now().minusMinutes(10).truncatedTo(ChronoUnit.MINUTES), "SCHEDULED");
        for(PostsEntity postsEntity : postsEntityList) {
            List<PostAccountsEntity> postAccountsEntityList = postAccountsRepo.findByPostId(postsEntity.getId());
            for(PostAccountsEntity postAccountsEntity : postAccountsEntityList) {
                if(postAccountsEntity.getStatus().equals("DELETED")) {
                    continue;
                }
                AccountsEntity accountEntity = accountsRepo.findById(postAccountsEntity.getAccountId()).get();
                runtimeConstants.channels.get(accountEntity.getAccountOf().toLowerCase()).post(accountEntity, postsEntity, true);
                postAccountsEntity.setStatus("PUBLISHED");
                postAccountsRepo.save(postAccountsEntity);
            }
            postsEntity.setStatus("PUBLISHED");
            postsRepo.save(postsEntity);
        }


    }

}
