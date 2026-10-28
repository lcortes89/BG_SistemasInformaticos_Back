package com.bgsistemas.api.summary;

import com.bgsistemas.api.summary.dtos.SummaryDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/summary")
public class AdminSummaryController {

    private final SummaryService summaryService;

    public AdminSummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    // GET /api/admin/summary -> counters + latest contacts for the dashboard
    @GetMapping
    public SummaryDTO getSummary() {
        return summaryService.getSummary();
    }
}
