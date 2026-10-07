package com.shopmanager.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Customer> findAllByOrderByCreatedAtDesc();

    @Query("""
            select c from Customer c
            where lower(c.email) like lower(concat('%', :query, '%'))
               or lower(c.name) like lower(concat('%', :query, '%'))
            order by c.createdAt desc
            """)
    List<Customer> search(@Param("query") String query);
}
