package com.shopmanager.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;

@Component
public class CurrentUser {

    private final CustomerService customerService;

    public CurrentUser(CustomerService customerService) {
        this.customerService = customerService;
    }

    public Customer require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Нужна авторизация");
        }
        return customerService.getByEmail(authentication.getName());
    }
}
