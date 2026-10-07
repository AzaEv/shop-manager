package com.shopmanager.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void allowsForwardAndCancelUntilShipped() {
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.PROCESSING));
        assertTrue(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    void forbidsSkippingAndReopening() {
        assertFalse(OrderStatus.NEW.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.NEW));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PAID));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED));
    }
}
