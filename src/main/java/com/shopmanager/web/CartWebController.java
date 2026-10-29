package com.shopmanager.web;

import java.net.URI;

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

import jakarta.servlet.http.HttpServletRequest;

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
            @RequestParam(defaultValue = "1") int quantity, RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        cartService.addItem(currentUser.require(authentication), productId, quantity);
        redirectAttributes.addFlashAttribute("message", "Товар добавлен в корзину");
        return redirectBack(request);
    }

    @PostMapping("/cart/items/{productId}")
    public String update(Authentication authentication, @PathVariable Long productId, @RequestParam int quantity) {
        cartService.updateQuantity(currentUser.require(authentication), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{productId}/delete")
    public String remove(Authentication authentication, @PathVariable Long productId,
            RedirectAttributes redirectAttributes) {
        cartService.removeItem(currentUser.require(authentication), productId);
        redirectAttributes.addFlashAttribute("message", "Товар убран из корзины");
        return "redirect:/cart";
    }

    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "redirect:/cart";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "redirect:/cart";
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return "redirect:/cart";
            }
            String query = uri.getRawQuery();
            return "redirect:" + path + (query == null || query.isBlank() ? "" : "?" + query);
        } catch (IllegalArgumentException ex) {
            return "redirect:/cart";
        }
    }
}
