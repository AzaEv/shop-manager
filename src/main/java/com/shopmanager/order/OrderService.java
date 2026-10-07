package com.shopmanager.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartItem;
import com.shopmanager.cart.CartService;
import com.shopmanager.catalog.Product;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;
import com.shopmanager.customer.Customer;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    public OrderService(OrderRepository orderRepository, CartService cartService) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    @Transactional(readOnly = true)
    public ShopOrder get(Long id) {
        ShopOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден: " + id));
        touch(order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<ShopOrder> listForCustomer(Customer customer) {
        List<ShopOrder> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(customer);
        orders.forEach(OrderService::touch);
        return orders;
    }

    @Transactional(readOnly = true)
    public List<ShopOrder> listAll(OrderStatus status) {
        List<ShopOrder> orders = status == null
                ? orderRepository.findAllByOrderByCreatedAtDesc()
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        orders.forEach(OrderService::touch);
        return orders;
    }

    @Transactional
    public ShopOrder checkout(Customer customer, String address) {
        Cart cart = cartService.getFor(customer);
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Корзина пуста");
        }

        ShopOrder order = new ShopOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.NEW);
        order.setAddress(address);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (!product.isActive()) {
                throw new BusinessException("Товар недоступен: " + product.getName());
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException("Недостаточно товара «" + product.getName() + "». В наличии: "
                        + product.getStock());
            }
            product.setStock(product.getStock() - cartItem.getQuantity());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotal(total);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.NEW);
        order.getHistory().add(history);

        cartService.clear(cart);
        return orderRepository.save(order);
    }

    private static void touch(ShopOrder order) {
        order.getItems().forEach(item -> {
            if (item.getProduct() != null) {
                item.getProduct().getStock();
            }
        });
        order.getHistory().size();
        order.getCustomer().getEmail();
    }

    @Transactional
    public ShopOrder changeStatus(Long orderId, OrderStatus next) {
        ShopOrder order = get(orderId);
        OrderStatus current = order.getStatus();
        if (!current.canTransitionTo(next)) {
            throw new BusinessException("Нельзя сменить статус " + current + " → " + next);
        }
        if (next == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                }
            }
        }
        order.setStatus(next);
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setFromStatus(current);
        history.setToStatus(next);
        order.getHistory().add(history);
        return order;
    }
}
