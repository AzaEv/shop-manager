package com.shopmanager.report;

import java.math.BigDecimal;
import java.util.List;

import com.shopmanager.order.OrderStatus;

public class ReportDtos {

    public record StatusRow(OrderStatus status, long orderCount, BigDecimal amount, int percentOfRevenue) {
    }

    public record ProductRow(String productName, long quantity, BigDecimal amount) {
    }

    public record ShopAnalytics(
            long customerCount,
            long productCount,
            long orderCount,
            long cancelledCount,
            BigDecimal revenue,
            BigDecimal averageCheck,
            List<StatusRow> byStatus,
            List<ProductRow> topProducts) {
    }
}
