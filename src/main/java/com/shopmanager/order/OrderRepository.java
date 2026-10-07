package com.shopmanager.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.shopmanager.customer.Customer;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findByCustomerOrderByCreatedAtDesc(Customer customer);

    List<ShopOrder> findAllByOrderByCreatedAtDesc();

    List<ShopOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    long countByStatus(OrderStatus status);

    @Query("""
            select o.status, count(o), coalesce(sum(o.total), 0)
            from ShopOrder o
            group by o.status
            """)
    List<Object[]> totalsByStatus();

    @Query("""
            select coalesce(sum(o.total), 0)
            from ShopOrder o
            where o.status <> com.shopmanager.order.OrderStatus.CANCELLED
            """)
    BigDecimal revenueExcludingCancelled();

    @Query("""
            select i.productName, sum(i.quantity), sum(i.unitPrice * i.quantity)
            from OrderItem i
            group by i.productName
            order by sum(i.unitPrice * i.quantity) desc
            """)
    List<Object[]> topProducts();
}
