package com.shopmanager.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanager.report.ReportDtos.ShopAnalytics;
import com.shopmanager.report.ReportService;

@RestController
@RequestMapping("/api/admin")
public class ReportApiController {

    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ShopAnalytics report() {
        return reportService.build();
    }
}
