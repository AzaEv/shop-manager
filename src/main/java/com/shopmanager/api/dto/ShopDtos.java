package com.shopmanager.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.shopmanager.customer.Role;
import com.shopmanager.order.OrderStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ShopDtos {

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank String name,
            String phone,
            @NotBlank String password) {
    }

    public record CustomerResponse(Long id, String email, String name, String phone, Role role, Instant createdAt) {
    }

    public record CartItemRequest(
            @NotNull Long productId,
            @Min(1) int quantity) {
    }

    public record QuantityChangeRequest(@Min(0) int quantity) {
    }

    public record CartItemResponse(Long productId, String name, String sku, BigDecimal price, int quantity,
            BigDecimal lineTotal) {
    }

    public record CartResponse(Long cartId, List<CartItemResponse> items, BigDecimal total) {
    }

    public record CheckoutRequest(@NotBlank String address) {
    }

    public record StatusRequest(@NotNull OrderStatus status) {
    }

    public record OrderItemResponse(String productName, String sku, BigDecimal unitPrice, int quantity) {
    }

    public record HistoryResponse(OrderStatus fromStatus, OrderStatus toStatus, Instant changedAt) {
    }

    public record OrderResponse(
            Long id,
            Long customerId,
            String customerEmail,
            OrderStatus status,
            BigDecimal total,
            String address,
            Instant createdAt,
            List<OrderItemResponse> items,
            List<HistoryResponse> history) {
    }
}
