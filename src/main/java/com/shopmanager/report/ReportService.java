package com.shopmanager.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.customer.CustomerRepository;
import com.shopmanager.order.OrderRepository;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.report.ReportDtos.ProductRow;
import com.shopmanager.report.ReportDtos.ShopAnalytics;
import com.shopmanager.report.ReportDtos.StatusRow;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public ReportService(OrderRepository orderRepository, CustomerRepository customerRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public ShopAnalytics build() {
        long customers = customerRepository.count();
        long products = productRepository.count();
        long orders = orderRepository.count();
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED);
        BigDecimal revenue = nvl(orderRepository.revenueExcludingCancelled());

        Map<OrderStatus, StatusRow> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            byStatus.put(status, new StatusRow(status, 0, BigDecimal.ZERO.setScale(2), 0));
        }
        for (Object[] row : orderRepository.totalsByStatus()) {
            OrderStatus status = (OrderStatus) row[0];
            long count = (Long) row[1];
            BigDecimal amount = nvl((BigDecimal) row[2]);
            int percent = percent(amount, revenue);
            byStatus.put(status, new StatusRow(status, count, amount, percent));
        }

        List<StatusRow> statusRows = byStatus.values().stream()
                .sorted(Comparator.comparing(StatusRow::status))
                .toList();

        List<ProductRow> top = new ArrayList<>();
        for (Object[] row : orderRepository.topProducts()) {
            top.add(new ProductRow((String) row[0], (Long) row[1], nvl((BigDecimal) row[2])));
            if (top.size() == 10) {
                break;
            }
        }

        BigDecimal activeOrders = BigDecimal.valueOf(Math.max(0, orders - cancelled));
        BigDecimal average = activeOrders.signum() == 0 || revenue.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : revenue.divide(activeOrders, 2, RoundingMode.HALF_UP);

        return new ShopAnalytics(customers, products, orders, cancelled, revenue, average, statusRows, top);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static int percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 0, RoundingMode.HALF_UP).intValue();
    }
}
