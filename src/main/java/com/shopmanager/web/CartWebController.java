package com.shopmanager.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartService;
import com.shopmanager.config.CurrentUser;
import com.shopmanager.customer.Customer;

@Controller
public class CartWebController {

    private final CartService cartService;
    private final CurrentUser currentUser;

    public CartWebController(CartService cartService, CurrentUser currentUser) {
        this.cartService = cartService;
        this.currentUser = currentUser;
    }

    @GetMapping("/cart")
    public String cart(Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        Cart cart = cartService.getFor(customer);
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.total(cart));
        model.addAttribute("pageTitle", "Корзина");
        return "shop/cart";
    }

    @PostMapping("/cart/items")
    public String add(Authentication authentication, @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity, RedirectAttributes redirectAttributes) {
        cartService.addItem(currentUser.require(authentication), productId, quantity);
        redirectAttributes.addFlashAttribute("message", "Товар добавлен в корзину");
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{productId}")
    public String update(Authentication authentication, @PathVariable Long productId, @RequestParam int quantity) {
        cartService.updateQuantity(currentUser.require(authentication), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{productId}/delete")
    public String remove(Authentication authentication, @PathVariable Long productId) {
        cartService.removeItem(currentUser.require(authentication), productId);
        return "redirect:/cart";
    }
}
