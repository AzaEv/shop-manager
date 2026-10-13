package com.shopmanager.customer;

public enum Role {
    ADMIN("Администратор"),
    MANAGER("Менеджер"),
    WAREHOUSE("Кладовщик"),
    CUSTOMER("Покупатель");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean managesOrders() {
        return this == ADMIN || this == MANAGER;
    }

    public boolean shops() {
        return this == CUSTOMER;
    }
}
