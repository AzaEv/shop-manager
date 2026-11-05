package com.shopmanager.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.shopmanager.report.ReportView.Sort;

@Service
public class ReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
    public ShopAnalytics build(ReportView view) {
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

        List<BreakdownRow> breakdown = breakdown(revenueOrders(), view, revenue);
        return new ShopAnalytics(customers, products, orders, cancelled, revenue, average, statusRows, top, breakdown);
    }

    public byte[] toCsv(ReportView view) {
        ShopAnalytics report = build(view);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("Показатель;Значение\n");
        line(csv, "Клиенты", Long.toString(report.customerCount()));
        line(csv, "Товары", Long.toString(report.productCount()));
        line(csv, "Заказы", Long.toString(report.orderCount()));
        line(csv, "Отменено", Long.toString(report.cancelledCount()));
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
        return OdsReport.write(build(view), orderRepository.findAllByOrderByCreatedAtDesc(), view);
    }

    private List<ShopOrder> revenueOrders() {
        Map<Long, ShopOrder> unique = new LinkedHashMap<>();
        for (ShopOrder order : orderRepository.findRevenueOrders()) {
            unique.putIfAbsent(order.getId(), order);
        }
        return List.copyOf(unique.values());
    }

    private List<BreakdownRow> breakdown(List<ShopOrder> orders, ReportView view, BigDecimal revenue) {
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (ShopOrder order : orders) {
            if (view.group() == Group.DAY) {
                LocalDate day = LocalDate.ofInstant(order.getCreatedAt(), ZONE);
                Bucket bucket = buckets.computeIfAbsent(day.toString(), key -> bucket(DAY.format(day), key));
                bucket.orders++;
                bucket.amount = bucket.amount.add(nvl(order.getTotal()));
                for (OrderItem item : order.getItems()) {
                    bucket.quantity += item.getQuantity();
                }
                continue;
            }
            for (OrderItem item : order.getItems()) {
                String label = view.group() == Group.CATEGORY ? categoryName(item) : item.getProductName();
                Bucket bucket = buckets.computeIfAbsent(label, key -> bucket(label, label));
                if (bucket.orderIds.add(order.getId())) {
                    bucket.orders++;
                }
                bucket.quantity += item.getQuantity();
                bucket.amount = bucket.amount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
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
