package com.shopmanager.config;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerRepository;

@Service
public class ShopUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public ShopUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(username.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
        return new User(
                customer.getEmail(),
                customer.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + customer.getRole().name())));
    }
}
