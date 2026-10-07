package com.shopmanager.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.cart.CartItemRepository;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;
import com.shopmanager.order.OrderItemRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
            CartItemRepository cartItemRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<Product> search(String query, Long categoryId, Boolean active, ProductSort sort, boolean ascending) {
        Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (sort == null ? ProductSort.NAME : sort) {
            case PRICE -> "price";
            case STOCK -> "stock";
            case NAME -> "name";
        };
        return productRepository.findAll(ProductSpecifications.search(query, categoryId, active),
                Sort.by(direction, property));
    }

    public List<Product> findActive(Long categoryId) {
        return search(null, categoryId, true, ProductSort.NAME, true);
    }

    public List<Product> findAll() {
        return search(null, null, null, ProductSort.NAME, true);
    }

    public Product get(Long id) {
        return productRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + id));
    }

    @Transactional
    public Product create(Long categoryId, String name, String sku, String description, BigDecimal price, int stock,
            boolean active) {
        if (productRepository.existsBySku(sku)) {
            throw new BusinessException("SKU уже занят: " + sku);
        }
        Product product = new Product();
        apply(product, categoryId, name, sku, description, price, stock, active);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Long categoryId, String name, String sku, String description, BigDecimal price,
            int stock, boolean active) {
        Product product = get(id);
        productRepository.findBySku(sku)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("SKU уже занят: " + sku);
                });
        apply(product, categoryId, name, sku, description, price, stock, active);
        return product;
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        if (cartItemRepository.countByProductId(id) > 0) {
            throw new BusinessException("Нельзя удалить товар «" + product.getName() + "»: он есть в корзине");
        }
        if (orderItemRepository.countByProductId(id) > 0) {
            throw new BusinessException("Нельзя удалить товар «" + product.getName()
                    + "»: он используется в заказах. Снимите флаг «активен».");
        }
        productRepository.delete(product);
    }

    private void apply(Product product, Long categoryId, String name, String sku, String description, BigDecimal price,
            int stock, boolean active) {
        if (price != null && price.signum() < 0) {
            throw new BusinessException("Цена не может быть отрицательной");
        }
        if (stock < 0) {
            throw new BusinessException("Остаток не может быть отрицательным");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + categoryId));
        product.setCategory(category);
        product.setName(name);
        product.setSku(sku.trim().toUpperCase(Locale.ROOT));
        product.setDescription(description);
        product.setPrice(price);
        product.setStock(stock);
        product.setActive(active);
    }
}
