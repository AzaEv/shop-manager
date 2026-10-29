package com.shopmanager.order;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    NEW,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            NEW, EnumSet.of(PAID, CANCELLED),
            PAID, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    public boolean canTransitionTo(OrderStatus next) {
        return TRANSITIONS.get(this).contains(next);
    }

    public String getTitle() {
        return switch (this) {
            case NEW -> "Новый";
            case PAID -> "Оплачен";
            case PROCESSING -> "Собирается";
            case SHIPPED -> "В пути";
            case DELIVERED -> "Доставлен";
            case CANCELLED -> "Отменён";
        };
    }

    public int getStep() {
        return switch (this) {
            case NEW -> 1;
            case PAID -> 2;
            case PROCESSING -> 3;
            case SHIPPED -> 4;
            case DELIVERED -> 5;
            case CANCELLED -> 0;
        };
    }
}
