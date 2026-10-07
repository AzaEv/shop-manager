package com.shopmanager.api;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanager.api.dto.ShopDtos.CartItemRequest;
import com.shopmanager.api.dto.ShopDtos.CartItemResponse;
import com.shopmanager.api.dto.ShopDtos.CartResponse;
import com.shopmanager.api.dto.ShopDtos.CheckoutRequest;
import com.shopmanager.api.dto.ShopDtos.CustomerResponse;
import com.shopmanager.api.dto.ShopDtos.HistoryResponse;
import com.shopmanager.api.dto.ShopDtos.OrderItemResponse;
import com.shopmanager.api.dto.ShopDtos.OrderResponse;
import com.shopmanager.api.dto.ShopDtos.QuantityChangeRequest;
import com.shopmanager.api.dto.ShopDtos.RegisterRequest;
import com.shopmanager.api.dto.ShopDtos.StatusRequest;
import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartItem;
import com.shopmanager.cart.CartService;
import com.shopmanager.config.CurrentUser;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;
import com.shopmanager.order.OrderService;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.order.ShopOrder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ShopApiController {

    private final CustomerService customerService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CurrentUser currentUser;

    public ShopApiController(CustomerService customerService, CartService cartService, OrderService orderService,
            CurrentUser currentUser) {
        this.customerService = customerService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.currentUser = currentUser;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse register(@Valid @RequestBody RegisterRequest request) {
        return toCustomer(customerService.register(
                request.email(), request.name(), request.phone(), request.password(), Role.CUSTOMER));
    }

    @GetMapping("/customers")
    public List<CustomerResponse> customers(@RequestParam(required = false) String q) {
        return customerService.search(q).stream().map(this::toCustomer).toList();
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse customer(@PathVariable Long id) {
        return toCustomer(customerService.get(id));
    }

    @GetMapping("/cart")
    public CartResponse cart(Authentication authentication) {
        return toCart(cartService.getFor(currentUser.require(authentication)));
    }

    @PostMapping("/cart/items")
    public CartResponse addItem(Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return toCart(cartService.addItem(currentUser.require(authentication), request.productId(), request.quantity()));
    }

    @PatchMapping("/cart/items/{productId}")
    public CartResponse updateItem(Authentication authentication, @PathVariable Long productId,
            @Valid @RequestBody QuantityChangeRequest request) {
        return toCart(cartService.updateQuantity(currentUser.require(authentication), productId, request.quantity()));
    }

    @DeleteMapping("/cart/items/{productId}")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long productId) {
        return toCart(cartService.removeItem(currentUser.require(authentication), productId));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        return toOrder(orderService.checkout(currentUser.require(authentication), request.address()));
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders(Authentication authentication,
            @RequestParam(required = false) OrderStatus status) {
        Customer customer = currentUser.require(authentication);
        List<ShopOrder> orders = customer.getRole() == Role.ADMIN
                ? orderService.listAll(status)
                : orderService.listForCustomer(customer);
        return orders.stream().map(this::toOrder).toList();
    }

    @GetMapping("/orders/{id}")
    public OrderResponse order(Authentication authentication, @PathVariable Long id) {
        ShopOrder order = orderService.get(id);
        ensureCanView(currentUser.require(authentication), order);
        return toOrder(order);
    }

    @PostMapping("/orders/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return toOrder(orderService.changeStatus(id, request.status()));
    }

    private void ensureCanView(Customer customer, ShopOrder order) {
        if (customer.getRole() != Role.ADMIN && !order.getCustomer().getId().equals(customer.getId())) {
            throw new com.shopmanager.common.NotFoundException("Заказ не найден: " + order.getId());
        }
    }

    private CustomerResponse toCustomer(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getEmail(),
                customer.getName(),
                customer.getPhone(),
                customer.getRole(),
                customer.getCreatedAt());
    }

    private CartResponse toCart(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(this::toCartItem).toList();
        return new CartResponse(cart.getId(), items, cartService.total(cart));
    }

    private CartItemResponse toCartItem(CartItem item) {
        BigDecimal line = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getProduct().getPrice(),
                item.getQuantity(),
                line);
    }

    private OrderResponse toOrder(ShopOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getEmail(),
                order.getStatus(),
                order.getTotal(),
                order.getAddress(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new OrderItemResponse(
                                item.getProductName(), item.getSku(), item.getUnitPrice(), item.getQuantity()))
                        .toList(),
                order.getHistory().stream()
                        .map(h -> new HistoryResponse(h.getFromStatus(), h.getToStatus(), h.getChangedAt()))
                        .toList());
    }
}
