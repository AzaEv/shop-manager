package com.shopmanager.report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.shopmanager.order.OrderStatus;

public record ReportView(
        Group group,
        Sort sort,
        boolean ascending,
        LocalDate from,
        LocalDate to,
        Set<OrderStatus> statuses,
        Long categoryId,
        int top) {

    public static final int DEFAULT_TOP = 10;
    public static final List<Integer> TOP_OPTIONS = List.of(5, 10, 20, 50);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ReportView {
        if (from != null && to != null && from.isAfter(to)) {
            LocalDate swap = from;
            from = to;
            to = swap;
        }
        statuses = statuses == null || statuses.isEmpty()
                ? EnumSet.allOf(OrderStatus.class)
                : EnumSet.copyOf(statuses);
        top = TOP_OPTIONS.contains(top) ? top : DEFAULT_TOP;
    }

    public enum Group {
        DAY("day", "По дням", "День"),
        MODEL("model", "По моделям", "Модель"),
        CATEGORY("category", "По категориям", "Категория");

        private final String code;
        private final String title;
        private final String column;

        Group(String code, String title, String column) {
            this.code = code;
            this.title = title;
            this.column = column;
        }

        public String code() {
            return code;
        }

        public String title() {
            return title;
        }

        public String column() {
            return column;
        }

        public static Group from(String raw) {
            if (raw == null || raw.isBlank()) {
                return DAY;
            }
            String value = raw.trim();
            for (Group group : values()) {
                if (group.code.equalsIgnoreCase(value)) {
                    return group;
                }
            }
            return DAY;
        }
    }

    public enum Sort {
        LABEL("label"),
        ORDERS("orders"),
        QUANTITY("quantity"),
        AMOUNT("amount");

        private final String code;

        Sort(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }

        public static Sort from(String raw) {
            if (raw == null || raw.isBlank()) {
                return LABEL;
            }
            String value = raw.trim();
            for (Sort sort : values()) {
                if (sort.code.equalsIgnoreCase(value)) {
                    return sort;
                }
            }
            return LABEL;
        }
    }

    public static ReportView of(String group, String sort, String dir) {
        return of(group, sort, dir, null, null, null, null, null);
    }

    public static ReportView of(String group, String sort, String dir, String from, String to,
            Collection<String> statuses, Long categoryId, Integer top) {
        boolean ascending = dir == null || dir.isBlank() || !"desc".equalsIgnoreCase(dir.trim());
        return new ReportView(Group.from(group), Sort.from(sort), ascending,
                date(from), date(to), statuses(statuses), categoryId, top == null ? DEFAULT_TOP : top);
    }

    public String dir() {
        return ascending ? "asc" : "desc";
    }

    public boolean includes(LocalDate day) {
        return (from == null || !day.isBefore(from)) && (to == null || !day.isAfter(to));
    }

    public boolean allStatuses() {
        return statuses.size() == OrderStatus.values().length;
    }

    public boolean filtered() {
        return from != null || to != null || !allStatuses() || categoryId != null;
    }

    public String periodTitle() {
        if (from == null && to == null) {
            return "за всё время";
        }
        if (from != null && to != null) {
            return from.equals(to) ? "за " + DAY.format(from) : "с " + DAY.format(from) + " по " + DAY.format(to);
        }
        return from != null ? "с " + DAY.format(from) : "по " + DAY.format(to);
    }

    public String fromParam() {
        return from == null ? null : from.toString();
    }

    public String toParam() {
        return to == null ? null : to.toString();
    }

    public List<String> statusParams() {
        return allStatuses() ? List.of() : statuses.stream().map(Enum::name).toList();
    }

    private static LocalDate date(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static Set<OrderStatus> statuses(Collection<String> raw) {
        Set<OrderStatus> result = EnumSet.noneOf(OrderStatus.class);
        if (raw == null) {
            return result;
        }
        for (String value : raw) {
            if (value == null) {
                continue;
            }
            for (String part : value.split(",")) {
                try {
                    result.add(OrderStatus.valueOf(part.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }
}
