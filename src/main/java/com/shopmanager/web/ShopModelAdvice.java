package com.shopmanager.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.shopmanager.admin.AdminWebController;
import com.shopmanager.cart.CartRepository;
import com.shopmanager.catalog.CategoryService;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;

@ControllerAdvice(assignableTypes = {
        ShopWebController.class,
        AuthWebController.class,
        CartWebController.class,
        OrderWebController.class,
        AdminWebController.class,
        ProfileWebController.class
})
public class ShopModelAdvice {

    private final CategoryService categoryService;
    private final CustomerService customerService;
    private final CartRepository cartRepository;

    public ShopModelAdvice(CategoryService categoryService, CustomerService customerService,
            CartRepository cartRepository) {
        this.categoryService = categoryService;
        this.customerService = customerService;
        this.cartRepository = cartRepository;
    }

    @ModelAttribute
    public void storefront(Model model, Authentication authentication) {
        Customer account = null;
        int cartCount = 0;
        if (isUser(authentication)) {
            account = customerService.findOptional(authentication.getName()).orElse(null);
            if (account != null && account.getRole() == Role.CUSTOMER) {
                cartCount = (int) cartRepository.sumQuantityByCustomerId(account.getId());
            }
        }
        model.addAttribute("navCategories", categoryService.findAll());
        model.addAttribute("account", account);
        model.addAttribute("cartCount", cartCount);
        if (!model.containsAttribute("q")) {
            model.addAttribute("q", "");
        }
    }

    private boolean isUser(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
