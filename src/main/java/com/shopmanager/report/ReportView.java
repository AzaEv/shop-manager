package com.shopmanager.report;

public record ReportView(Group group, Sort sort, boolean ascending) {

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
        boolean ascending = dir == null || dir.isBlank() || !"desc".equalsIgnoreCase(dir.trim());
        return new ReportView(Group.from(group), Sort.from(sort), ascending);
    }

    public String dir() {
        return ascending ? "asc" : "desc";
    }
}
