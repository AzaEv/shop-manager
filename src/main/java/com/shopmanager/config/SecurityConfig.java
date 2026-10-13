package com.shopmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2-console/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/products/**",
                                "/css/**",
                                "/login",
                                "/register",
                                "/error",
                                "/actuator/health",
                                "/h2-console/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register")
                        .permitAll()
                        .requestMatchers("/cart", "/cart/**", "/checkout", "/orders", "/orders/**",
                                "/api/cart", "/api/cart/**")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/orders")
                        .hasRole("CUSTOMER")
                        .requestMatchers("/admin/report.csv", "/api/admin/report.csv")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/customers/*/role")
                        .hasRole("ADMIN")
                        .requestMatchers("/admin/report", "/api/admin/report")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/orders", "/admin/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/status")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/admin/products", "/admin/products/**",
                                "/admin/categories", "/admin/categories/**",
                                "/api/admin/products", "/api/admin/products/**",
                                "/api/admin/categories", "/api/admin/categories/**")
                        .hasAnyRole("ADMIN", "WAREHOUSE")
                        .requestMatchers("/admin/customers", "/api/customers", "/api/customers/**")
                        .hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/admin")
                        .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE")
                        .requestMatchers("/admin/**", "/api/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .httpBasic(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
        return http.build();
    }
}
