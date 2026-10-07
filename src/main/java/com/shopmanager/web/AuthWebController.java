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

import com.shopmanager.config.ShopUserDetailsService;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AuthWebController {

    private final CustomerService customerService;
    private final ShopUserDetailsService userDetailsService;

    public AuthWebController(CustomerService customerService, ShopUserDetailsService userDetailsService) {
        this.customerService = customerService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Вход");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("pageTitle", "Регистрация");
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String email,
            @RequestParam String name,
            @RequestParam(required = false) String phone,
            @RequestParam String password,
            HttpServletRequest request) {
        customerService.register(email, name, phone, password, Role.CUSTOMER);
        UserDetails details = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                details, details.getPassword(), details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        return "redirect:/";
    }
}
