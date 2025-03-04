package com.socialeazy.api.controller;

import com.socialeazy.api.domains.responses.QuickStatsResponse;
import com.socialeazy.api.services.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("v1")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/quick-stats")
    public QuickStatsResponse getQuickStats(@RequestHeader int userId, @RequestHeader int orgId, @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime fromDateTime = LocalDateTime.parse(fromDate + " 00:00:00", formatter);
        LocalDateTime toDateTime = LocalDateTime.parse(toDate + " 23:59:59", formatter);
        return dashboardService.getQuickStats(userId, orgId, fromDateTime, toDateTime);
    }
}
