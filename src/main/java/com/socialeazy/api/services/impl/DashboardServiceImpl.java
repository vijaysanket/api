package com.socialeazy.api.services.impl;

import com.socialeazy.api.domains.responses.QuickStatItem;
import com.socialeazy.api.domains.responses.QuickStatsResponse;
import com.socialeazy.api.repo.AccountsRepo;
import com.socialeazy.api.repo.PostsRepo;
import com.socialeazy.api.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private PostsRepo postsRepo;

    @Autowired
    private AccountsRepo accountsRepo;
    @Override
    public QuickStatsResponse getQuickStats(int userId, int orgId, LocalDateTime fromDate, LocalDateTime endDate) {


        Period period = Period.between(fromDate.toLocalDate(), endDate.toLocalDate());

        LocalDateTime prevFromDate = fromDate.minusDays(period.getDays());
        LocalDateTime prevEndDate = endDate.minusDays(period.getDays());

        int currentCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "SCHEDULED", fromDate, endDate);
        int prevCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "SCHEDULED", prevFromDate, prevEndDate);

        QuickStatItem scheduled = new QuickStatItem();
        scheduled.setCount(currentCount);
        try {
            scheduled.setChange(((currentCount-prevCount)/prevCount)*100);
        } catch (ArithmeticException e) {
            scheduled.setChange(0);
        }

        currentCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "PUBLISHED", fromDate, endDate);
        prevCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "PUBLISHED", prevFromDate, prevEndDate);

        QuickStatItem published = new QuickStatItem();
        published.setCount(currentCount);
        try {
            published.setChange(((currentCount-prevCount)/prevCount)*100);
        } catch (ArithmeticException e) {
            published.setChange(0);
        }


        currentCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "DRAFT", fromDate, endDate);
        prevCount = postsRepo.findCountByOrgIdAndStatusAndScheduledBetween(orgId, "DRAFT", prevFromDate, prevEndDate);

        QuickStatItem draft = new QuickStatItem();
        draft.setCount(currentCount);
        try {
            draft.setChange(((currentCount-prevCount)/prevCount)*100);
        } catch (ArithmeticException e) {
            draft.setChange(0);
        }

        //TODO :: Add engagemnet related code here
        QuickStatItem engagement = new QuickStatItem();
        engagement.setCount(0);
        try {
            engagement.setChange(0);
        } catch (ArithmeticException e) {
            engagement.setChange(0);
        }
        QuickStatsResponse quickStatsResponse = new QuickStatsResponse();

        try {
            quickStatsResponse.setFollowersCount(accountsRepo.findCountByOrgIdAndActive(orgId, true));
        } catch (NullPointerException e) {
            quickStatsResponse.setFollowersCount(0);
        }
        quickStatsResponse.setPublishedPosts(published);
        quickStatsResponse.setEngagementRate(engagement);
        quickStatsResponse.setScheduledPosts(scheduled);
        quickStatsResponse.setDraftPosts(draft);
        return quickStatsResponse;
    }
}
