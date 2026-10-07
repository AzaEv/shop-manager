package com.shopmanager.cart;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;
import com.shopmanager.customer.Customer;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Cart getFor(Customer customer) {
        return getOrCreate(customer);
    }

    @Transactional
    public Cart addItem(Customer customer, Long productId, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("Количество должно быть больше нуля");
        }
        Cart cart = getOrCreate(customer);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Товар не найден: " + productId));
        if (!product.isActive()) {
            throw new BusinessException("Товар недоступен");
        }
        CartItem item = cart.getItems().stream()
                .filter(existing -> existing.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
        int nextQuantity = quantity + (item == null ? 0 : item.getQuantity());
        ensureStock(product, nextQuantity);
        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        } else {
            item.setQuantity(nextQuantity);
        }
        return cart;
    }

    @Transactional
    public Cart updateQuantity(Customer customer, Long productId, int quantity) {
        Cart cart = getOrCreate(customer);
        CartItem item = findItem(cart, productId);
        if (quantity < 1) {
            cart.getItems().remove(item);
            item.setCart(null);
            return cart;
        }
        ensureStock(item.getProduct(), quantity);
        item.setQuantity(quantity);
        return cart;
    }

    @Transactional
    public Cart removeItem(Customer customer, Long productId) {
        Cart cart = getOrCreate(customer);
        CartItem item = findItem(cart, productId);
        cart.getItems().remove(item);
        item.setCart(null);
        return cart;
    }

    @Transactional
    public void clear(Cart cart) {
        cart.getItems().clear();
    }

    public BigDecimal total(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Cart getOrCreate(Customer customer) {
        return cartRepository.findDetailedByCustomerId(customer.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }

    private CartItem findItem(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Товара нет в корзине"));
    }

    private void ensureStock(Product product, int quantity) {
        if (product.getStock() < quantity) {
            throw new BusinessException("Недостаточно товара «" + product.getName() + "». В наличии: "
                    + product.getStock());
        }
    }
}
