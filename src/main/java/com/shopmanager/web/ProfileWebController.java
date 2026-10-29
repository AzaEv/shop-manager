package com.shopmanager.web;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shopmanager.config.CurrentUser;
import com.shopmanager.config.ShopUserDetailsService;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ProfileWebController {

    private final CurrentUser currentUser;
    private final CustomerService customerService;
    private final ShopUserDetailsService userDetailsService;

    public ProfileWebController(CurrentUser currentUser, CustomerService customerService,
            ShopUserDetailsService userDetailsService) {
        this.currentUser = currentUser;
        this.customerService = customerService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/profile")
    public String profile(org.springframework.security.core.Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        model.addAttribute("customer", customer);
        model.addAttribute("pageTitle", "Профиль");
        return "shop/profile";
    }

    @PostMapping("/profile")
    public String update(
            org.springframework.security.core.Authentication authentication,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Customer actor = currentUser.require(authentication);
        Customer updated = customerService.updateProfile(
                actor.getId(), name, email, phone, address, currentPassword, newPassword);
        UserDetails details = userDetailsService.loadUserByUsername(updated.getEmail());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, details.getPassword(), details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        redirectAttributes.addFlashAttribute("message", "Профиль сохранён");
        return "redirect:/profile";
    }
}
