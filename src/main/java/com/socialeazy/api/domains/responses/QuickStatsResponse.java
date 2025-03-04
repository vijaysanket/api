package com.socialeazy.api.domains.responses;

import lombok.Data;

@Data
public class QuickStatsResponse {
    private QuickStatItem scheduledPosts;

    private QuickStatItem publishedPosts;

    private QuickStatItem engagementRate;

    private QuickStatItem draftPosts;

    private int followersCount;
}
