package com.shopmanager.catalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @EntityGraph(attributePaths = "category")
    List<Product> findAll(org.springframework.data.jpa.domain.Specification<Product> spec,
            org.springframework.data.domain.Sort sort);

    @Query("select p from Product p join fetch p.category where p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    long countByCategoryId(Long categoryId);
}
