package com.shopmanager.customer;

import java.util.List;
import java.util.Optional;

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
        return customerRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new NotFoundException("Клиент не найден: " + email));
    }

    public Optional<Customer> findOptional(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return customerRepository.findByEmail(normalizeEmail(email));
    }

    public boolean exists(String email) {
        return customerRepository.existsByEmail(normalizeEmail(email));
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
        String normalized = normalizeEmail(email);
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

    @Transactional
    public void changeRole(Customer actor, Long customerId, Role role) {
        if (actor.getRole() != Role.ADMIN) {
            throw new BusinessException("Менять роли может только администратор");
        }
        Customer customer = get(customerId);
        if (actor.getId().equals(customer.getId())) {
            throw new BusinessException("Нельзя изменить собственную роль");
        }
        if (customer.getRole() == Role.ADMIN && role != Role.ADMIN
                && customerRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessException("Нельзя снять роль с последнего администратора");
        }
        customer.setRole(role);
    }

    @Transactional
    public Customer updateProfile(Long id, String name, String email, String phone, String address,
            String currentPassword, String newPassword) {
        Customer customer = get(id);
        String nextName = requireText(name, 180, "Укажите имя", "Имя слишком длинное");
        String nextEmail = normalizeEmail(email);
        if (nextEmail.length() > 180 || !nextEmail.contains("@") || nextEmail.startsWith("@")
                || nextEmail.endsWith("@") || !nextEmail.contains(".")) {
            throw new BusinessException("Укажите корректный email");
        }
        String nextPhone = optional(phone, 40, "Телефон слишком длинный");
        String nextAddress = optional(address, 500, "Адрес слишком длинный");
        String nextPassword = newPassword == null ? "" : newPassword.trim();
        boolean emailChanged = !nextEmail.equals(customer.getEmail());
        boolean passwordChanged = !nextPassword.isEmpty();
        if (emailChanged || passwordChanged) {
            if (currentPassword == null || currentPassword.isBlank()
                    || !passwordEncoder.matches(currentPassword, customer.getPasswordHash())) {
                throw new BusinessException("Неверный текущий пароль");
            }
        }
        if (passwordChanged && nextPassword.length() < 6) {
            throw new BusinessException("Новый пароль должен быть не короче 6 символов");
        }
        if (emailChanged && customerRepository.existsByEmail(nextEmail)) {
            throw new BusinessException("Email уже зарегистрирован: " + nextEmail);
        }
        customer.setName(nextName);
        customer.setEmail(nextEmail);
        customer.setPhone(nextPhone);
        customer.setAddress(nextAddress);
        if (passwordChanged) {
            customer.setPasswordHash(passwordEncoder.encode(nextPassword));
        }
        return customer;
    }

    @Transactional
    public void saveAddress(Long customerId, String address) {
        Customer customer = get(customerId);
        customer.setAddress(optional(address, 500, "Адрес слишком длинный"));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Укажите корректный email");
        }
        return email.toLowerCase().trim();
    }

    private String requireText(String value, int max, String emptyMessage, String longMessage) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(emptyMessage);
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(longMessage);
        }
        return trimmed;
    }

    private String optional(String value, int max, String longMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new BusinessException(longMessage);
        }
        return trimmed;
    }
}
