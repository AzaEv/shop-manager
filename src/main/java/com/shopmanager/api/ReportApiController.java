package com.shopmanager.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanager.report.ReportDtos.ShopAnalytics;
import com.shopmanager.report.ReportService;
import com.shopmanager.report.ReportView;

@RestController
@RequestMapping("/api/admin")
public class ReportApiController {

    private final ReportService reportService;

    public ReportApiController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/report")
    public ShopAnalytics report(
            @RequestParam(defaultValue = "day") String group,
            @RequestParam(defaultValue = "label") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(name = "status", required = false) List<String> statuses,
            @RequestParam(name = "category", required = false) Long categoryId,
            @RequestParam(required = false) Integer top) {
        return reportService.build(ReportView.of(group, sort, dir, from, to, statuses, categoryId, top));
    }
}
