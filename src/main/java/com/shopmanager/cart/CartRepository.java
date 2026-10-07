package com.shopmanager.cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopmanager.customer.Customer;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomer(Customer customer);

    @Query("""
            select distinct c from Cart c
            left join fetch c.items i
            left join fetch i.product p
            left join fetch p.category
            where c.customer.id = :customerId
            """)
    Optional<Cart> findDetailedByCustomerId(@Param("customerId") Long customerId);
}
