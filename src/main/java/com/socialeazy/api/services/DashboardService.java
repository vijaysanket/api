package com.socialeazy.api.services;

import com.socialeazy.api.domains.responses.QuickStatsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public interface DashboardService {
    QuickStatsResponse getQuickStats(int userId, int orgId, LocalDateTime fromDate, LocalDateTime endDate);
}
