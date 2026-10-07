package com.shopmanager.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartService;
import com.shopmanager.common.NotFoundException;
import com.shopmanager.config.CurrentUser;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.Role;
import com.shopmanager.order.OrderService;
import com.shopmanager.order.ShopOrder;

@Controller
public class OrderWebController {

    private final OrderService orderService;
    private final CartService cartService;
    private final CurrentUser currentUser;

    public OrderWebController(OrderService orderService, CartService cartService, CurrentUser currentUser) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping("/checkout")
    public String checkoutForm(Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        Cart cart = cartService.getFor(customer);
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.total(cart));
        model.addAttribute("pageTitle", "Оформление заказа");
        return "shop/checkout";
    }

    @PostMapping("/checkout")
    public String checkout(Authentication authentication, @RequestParam String address) {
        ShopOrder order = orderService.checkout(currentUser.require(authentication), address);
        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/orders")
    public String myOrders(Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        model.addAttribute("orders", orderService.listForCustomer(customer));
        model.addAttribute("pageTitle", "Мои заказы");
        return "shop/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = orderService.get(id);
        if (customer.getRole() != Role.ADMIN && !order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Заказ не найден: " + id);
        }
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "Заказ №" + order.getId());
        return "shop/order";
    }
}
