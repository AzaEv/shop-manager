package com.shopmanager.catalog;

public enum ProductSort {
    NAME,
    PRICE,
    STOCK;

    public static ProductSort from(String value) {
        if (value == null || value.isBlank()) {
            return NAME;
        }
        try {
            return ProductSort.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NAME;
        }
    }
}
