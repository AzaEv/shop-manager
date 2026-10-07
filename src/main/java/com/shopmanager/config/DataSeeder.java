package com.shopmanager.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryRepository;
import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerService customerService;

    public DataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
            CustomerService customerService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerService = customerService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category phones = category("Смартфоны", "phones");
        Category audio = category("Аудио", "audio");
        Category office = category("Офис", "office");

        product(phones, "Nova X15", "PHN-X15", "Смартфон 6.5\", 128 ГБ", "34990.00", 12);
        product(phones, "Nova X15 Pro", "PHN-X15P", "Смартфон 6.7\", 256 ГБ", "49990.00", 8);
        product(audio, "Wave Buds", "AUD-WB1", "Беспроводные наушники", "5990.00", 30);
        product(audio, "Wave Speaker", "AUD-WS1", "Портативная колонка", "7990.00", 15);
        product(office, "Desk Lamp", "OFF-DL2", "Настольная лампа", "2490.00", 20);
        product(office, "Notebook A5", "OFF-NB5", "Блокнот A5, 120 листов", "390.00", 50);

        customerService.register("admin@shop.local", "Администратор", "+70000000000", "admin123", Role.ADMIN);
        customerService.register("customer@shop.local", "Иван Покупатель", "+70000000001", "customer123",
                Role.CUSTOMER);
    }

    private Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    private void product(Category category, String name, String sku, String description, String price, int stock) {
        Product product = new Product();
        product.setCategory(category);
        product.setName(name);
        product.setSku(sku);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setActive(true);
        productRepository.save(product);
    }
}
