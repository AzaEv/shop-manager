package com.shopmanager.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryRepository;
import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.customer.CustomerRepository;
import com.shopmanager.order.OrderItem;
import com.shopmanager.order.OrderRepository;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.order.ShopOrder;
import com.shopmanager.report.ReportDtos.BreakdownRow;
import com.shopmanager.report.ReportDtos.ProductRow;
import com.shopmanager.report.ReportDtos.ShopAnalytics;
import com.shopmanager.report.ReportDtos.StatusRow;
import com.shopmanager.report.ReportView.Group;

@Service
public class ReportService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Instant MIN = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant MAX = Instant.parse("9999-01-01T00:00:00Z");
    private static final int MAX_EMPTY_DAYS = 366;

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ReportService(OrderRepository orderRepository, CustomerRepository customerRepository,
            ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public ShopAnalytics build(ReportView view) {
        return analyze(orders(view), view);
    }

    @Transactional(readOnly = true)
    public List<ShopOrder> orders(ReportView view) {
        Instant start = view.from() == null ? MIN : view.from().atStartOfDay(ZONE).toInstant();
        Instant end = view.to() == null ? MAX : view.to().plusDays(1).atStartOfDay(ZONE).toInstant();
        return orderRepository.findForReport(start, end).stream()
                .filter(order -> view.statuses().contains(order.getStatus()))
                .filter(order -> !lines(order, view).isEmpty() || view.categoryId() == null)
                .toList();
    }

    public String categoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId).map(Category::getName).orElse(null);
    }

    public String describe(ReportView view) {
        StringBuilder text = new StringBuilder("Период ").append(view.periodTitle());
        String category = categoryName(view.categoryId());
        if (category != null) {
            text.append(" · категория «").append(category).append('»');
        }
        if (!view.allStatuses()) {
            text.append(" · статусы: ").append(String.join(", ",
                    view.statuses().stream().map(OrderStatus::getTitle).toList()));
        }
        return text.toString();
    }

    private ShopAnalytics analyze(List<ShopOrder> orders, ReportView view) {
        Map<OrderStatus, StatusAcc> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : view.statuses()) {
            byStatus.put(status, new StatusAcc());
        }
        BigDecimal revenue = BigDecimal.ZERO;
        long cancelled = 0;
        long itemsSold = 0;
        Set<Long> buyers = new HashSet<>();
        List<ShopOrder> revenueOrders = new ArrayList<>();
        Map<String, Bucket> products = new LinkedHashMap<>();

        for (ShopOrder order : orders) {
            BigDecimal amount = amount(order, view);
            StatusAcc acc = byStatus.computeIfAbsent(order.getStatus(), key -> new StatusAcc());
            acc.count++;
            acc.amount = acc.amount.add(amount);
            if (order.getStatus() == OrderStatus.CANCELLED) {
                cancelled++;
                continue;
            }
            revenue = revenue.add(amount);
            revenueOrders.add(order);
            buyers.add(order.getCustomer().getId());
            for (OrderItem item : lines(order, view)) {
                itemsSold += item.getQuantity();
                Bucket bucket = products.computeIfAbsent(item.getProductName(), key -> bucket(key, key));
                bucket.quantity += item.getQuantity();
                bucket.amount = bucket.amount.add(line(item));
            }
        }

        BigDecimal total = nvl(revenue);
        List<StatusRow> statusRows = byStatus.entrySet().stream()
                .map(entry -> new StatusRow(entry.getKey(), entry.getValue().count, nvl(entry.getValue().amount),
                        percent(nvl(entry.getValue().amount), total)))
                .sorted(Comparator.comparing(StatusRow::status))
                .toList();

        List<ProductRow> top = products.values().stream()
                .sorted(Comparator.comparing((Bucket bucket) -> bucket.amount).reversed()
                        .thenComparing(bucket -> bucket.sortKey, labelOrder()))
                .limit(view.top())
                .map(bucket -> new ProductRow(bucket.label, bucket.quantity, nvl(bucket.amount)))
                .toList();

        long active = revenueOrders.size();
        BigDecimal average = active == 0 || total.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(active), 2, RoundingMode.HALF_UP);

        return new ShopAnalytics(
                customerRepository.count(),
                productRepository.count(),
                orders.size(),
                cancelled,
                total,
                average,
                statusRows,
                top,
                breakdown(revenueOrders, view, total),
                buyers.size(),
                itemsSold,
                describe(view));
    }

    public byte[] toCsv(ReportView view) {
        ShopAnalytics report = build(view);
        StringBuilder csv = new StringBuilder();
        csv.append('﻿');
        line(csv, "Отчёт", report.period());
        csv.append('\n');
        csv.append("Показатель;Значение\n");
        line(csv, "Клиенты", Long.toString(report.customerCount()));
        line(csv, "Покупатели за период", Long.toString(report.buyerCount()));
        line(csv, "Товары", Long.toString(report.productCount()));
        line(csv, "Заказы", Long.toString(report.orderCount()));
        line(csv, "Отменено", Long.toString(report.cancelledCount()));
        line(csv, "Продано, шт.", Long.toString(report.itemsSold()));
        line(csv, "Выручка, руб.", money(report.revenue()));
        line(csv, "Средний чек, руб.", money(report.averageCheck()));
        csv.append('\n');
        csv.append("Статус;Заказов;Сумма, руб.;Доля выручки, %\n");
        for (StatusRow status : report.byStatus()) {
            csv.append(escape(status.status().getTitle())).append(';')
                    .append(status.orderCount()).append(';')
                    .append(money(status.amount())).append(';')
                    .append(status.percentOfRevenue())
                    .append('\n');
        }
        csv.append('\n');
        csv.append("Товар;Продано, шт.;Сумма, руб.\n");
        for (ProductRow product : report.topProducts()) {
            csv.append(escape(product.productName())).append(';')
                    .append(product.quantity()).append(';')
                    .append(money(product.amount()))
                    .append('\n');
        }
        csv.append('\n');
        csv.append("Сравнение выручки: ").append(escape(view.group().title())).append('\n');
        csv.append(escape(view.group().column())).append(";Заказов;Продано, шт.;Выручка, руб.;Доля выручки, %\n");
        for (BreakdownRow row : report.breakdown()) {
            csv.append(escape(row.label())).append(';')
                    .append(row.orderCount()).append(';')
                    .append(row.quantity()).append(';')
                    .append(money(row.amount())).append(';')
                    .append(row.percentOfRevenue())
                    .append('\n');
        }
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] toOds(ReportView view) {
        List<ShopOrder> orders = orders(view);
        return OdsReport.write(analyze(orders, view), orders, view);
    }

    private List<BreakdownRow> breakdown(List<ShopOrder> orders, ReportView view, BigDecimal revenue) {
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        if (view.group() == Group.DAY && view.from() != null && view.to() != null
                && ChronoUnit.DAYS.between(view.from(), view.to()) < MAX_EMPTY_DAYS) {
            for (LocalDate day = view.from(); !day.isAfter(view.to()); day = day.plusDays(1)) {
                LocalDate current = day;
                buckets.put(day.toString(), bucket(DAY.format(current), current.toString()));
            }
        }
        for (ShopOrder order : orders) {
            List<OrderItem> lines = lines(order, view);
            if (view.group() == Group.DAY) {
                LocalDate day = LocalDate.ofInstant(order.getCreatedAt(), ZONE);
                Bucket bucket = buckets.computeIfAbsent(day.toString(), key -> bucket(DAY.format(day), key));
                bucket.orders++;
                bucket.amount = bucket.amount.add(amount(order, view));
                for (OrderItem item : lines) {
                    bucket.quantity += item.getQuantity();
                }
                continue;
            }
            for (OrderItem item : lines) {
                String label = view.group() == Group.CATEGORY ? categoryName(item) : item.getProductName();
                Bucket bucket = buckets.computeIfAbsent(label, key -> bucket(label, label));
                if (bucket.orderIds.add(order.getId())) {
                    bucket.orders++;
                }
                bucket.quantity += item.getQuantity();
                bucket.amount = bucket.amount.add(line(item));
            }
        }
        Comparator<Bucket> comparator = switch (view.sort()) {
            case ORDERS -> Comparator.comparingLong(bucket -> bucket.orders);
            case QUANTITY -> Comparator.comparingLong(bucket -> bucket.quantity);
            case AMOUNT -> Comparator.comparing(bucket -> bucket.amount);
            case LABEL -> Comparator.comparing(bucket -> bucket.sortKey, labelOrder());
        };
        if (!view.ascending()) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(bucket -> bucket.sortKey, labelOrder());
        return buckets.values().stream()
                .sorted(comparator)
                .map(bucket -> new BreakdownRow(
                        bucket.label,
                        bucket.orders,
                        bucket.quantity,
                        nvl(bucket.amount),
                        percent(nvl(bucket.amount), revenue)))
                .toList();
    }

    private static List<OrderItem> lines(ShopOrder order, ReportView view) {
        if (view.categoryId() == null) {
            return order.getItems();
        }
        return order.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getCategory() != null
                        && Objects.equals(item.getProduct().getCategory().getId(), view.categoryId()))
                .toList();
    }

    private static BigDecimal amount(ShopOrder order, ReportView view) {
        if (view.categoryId() == null) {
            return nvl(order.getTotal());
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : lines(order, view)) {
            sum = sum.add(line(item));
        }
        return sum;
    }

    private static BigDecimal line(OrderItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private static Bucket bucket(String label, String sortKey) {
        Bucket bucket = new Bucket();
        bucket.label = label;
        bucket.sortKey = sortKey;
        return bucket;
    }

    private static String categoryName(OrderItem item) {
        Product product = item.getProduct();
        if (product == null || product.getCategory() == null) {
            return "Без категории";
        }
        return product.getCategory().getName();
    }

    private static Comparator<String> labelOrder() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("ru"));
        collator.setStrength(Collator.PRIMARY);
        return Comparator.nullsLast(collator);
    }

    private static final class StatusAcc {
        private long count;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    private static final class Bucket {
        private String label;
        private String sortKey;
        private long orders;
        private long quantity;
        private BigDecimal amount = BigDecimal.ZERO;
        private final Set<Long> orderIds = new HashSet<>();
    }

    private static void line(StringBuilder csv, String name, String value) {
        csv.append(escape(name)).append(';').append(escape(value)).append('\n');
    }

    private static String money(BigDecimal value) {
        return nvl(value).toPlainString().replace('.', ',');
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(";") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
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
