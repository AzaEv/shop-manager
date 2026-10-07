package com.shopmanager.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> search(String query, Long categoryId, Boolean active) {
        return (root, cq, cb) -> {
            if (cq != null && Product.class.equals(cq.getResultType())) {
                root.fetch("category");
                cq.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (Boolean.TRUE.equals(active)) {
                predicates.add(cb.isTrue(root.get("active")));
            } else if (Boolean.FALSE.equals(active)) {
                predicates.add(cb.isFalse(root.get("active")));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
