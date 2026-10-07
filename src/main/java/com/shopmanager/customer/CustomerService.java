package com.shopmanager.customer;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartRepository;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, CartRepository cartRepository,
            PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.cartRepository = cartRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Customer get(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент не найден: " + id));
    }

    public Customer getByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Клиент не найден: " + email));
    }

    public List<Customer> findAll() {
        return customerRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Customer> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return customerRepository.search(query.trim());
    }

    @Transactional
    public Customer register(String email, String name, String phone, String rawPassword, Role role) {
        String normalized = email.toLowerCase().trim();
        if (customerRepository.existsByEmail(normalized)) {
            throw new BusinessException("Email уже зарегистрирован: " + email);
        }
        Customer customer = new Customer();
        customer.setEmail(normalized);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setPasswordHash(passwordEncoder.encode(rawPassword));
        customer.setRole(role);
        customerRepository.save(customer);

        Cart cart = new Cart();
        cart.setCustomer(customer);
        cartRepository.save(cart);
        return customer;
    }
}
