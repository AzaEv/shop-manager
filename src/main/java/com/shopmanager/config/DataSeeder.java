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
        Category phones = ensureCategory("Смартфоны", "phones");
        Category audio = ensureCategory("Аудио", "audio");
        Category office = ensureCategory("Офис", "office");
        Category accessories = ensureCategory("Аксессуары", "accessories");
        Category home = ensureCategory("Для дома", "home");

        ensureProduct(phones, "Nova X15", "PHN-X15", "Смартфон 6.5\", 128 ГБ", "34990.00", 12);
        ensureProduct(phones, "Nova X15 Pro", "PHN-X15P", "Смартфон 6.7\", 256 ГБ", "49990.00", 8);
        ensureProduct(phones, "Nova Air", "PHN-AIR", "Лёгкий смартфон 6.1\", 128 ГБ", "27990.00", 18);
        ensureProduct(phones, "Nova Mini", "PHN-MINI", "Компактный смартфон 5.4\"", "19990.00", 22);
        ensureProduct(audio, "Wave Buds", "AUD-WB1", "Беспроводные наушники", "5990.00", 30);
        ensureProduct(audio, "Wave Speaker", "AUD-WS1", "Портативная колонка", "7990.00", 15);
        ensureProduct(audio, "Wave Headphones", "AUD-WH1", "Полноразмерные наушники", "8990.00", 14);
        ensureProduct(audio, "Wave Mic", "AUD-MIC", "Настольный микрофон", "6490.00", 9);
        ensureProduct(office, "Desk Lamp", "OFF-DL2", "Настольная лампа", "2490.00", 20);
        ensureProduct(office, "Notebook A5", "OFF-NB5", "Блокнот A5, 120 листов", "390.00", 50);
        ensureProduct(office, "Pen Set", "OFF-PEN", "Набор ручек, 5 шт.", "490.00", 40);
        ensureProduct(office, "Mouse Pad", "OFF-MP1", "Коврик для мыши", "790.00", 25);
        ensureProduct(office, "Office Chair", "OFF-CH1", "Офисное кресло", "15990.00", 6);
        ensureProduct(accessories, "USB-C Cable", "ACC-USBC", "Кабель USB-C, 1 м", "690.00", 60);
        ensureProduct(accessories, "Power Bank", "ACC-PB1", "Внешний аккумулятор 10000 мА·ч", "2990.00", 16);
        ensureProduct(accessories, "Phone Case", "ACC-CS1", "Чехол для Nova X15", "990.00", 35);
        ensureProduct(home, "Ceramic Mug", "HOM-MUG", "Керамическая кружка", "590.00", 40);
        ensureProduct(home, "Desk Plant", "HOM-PLT", "Небольшое растение для стола", "1290.00", 11);

        ensureUser("admin@shop.local", "Администратор", "+70000000000", "admin123", Role.ADMIN);
        ensureUser("manager@shop.local", "Мария Менеджер", "+70000000002", "manager123", Role.MANAGER);
        ensureUser("warehouse@shop.local", "Пётр Кладовщик", "+70000000003", "warehouse123", Role.WAREHOUSE);
        ensureUser("customer@shop.local", "Иван Покупатель", "+70000000001", "customer123", Role.CUSTOMER);
    }

    private void ensureUser(String email, String name, String phone, String password, Role role) {
        if (!customerService.exists(email)) {
            customerService.register(email, name, phone, password, role);
        }
    }

    private Category ensureCategory(String name, String slug) {
        return categoryRepository.findBySlug(slug).orElseGet(() -> category(name, slug));
    }

    private void ensureProduct(Category category, String name, String sku, String description, String price, int stock) {
        if (!productRepository.existsBySku(sku)) {
            product(category, name, sku, description, price, stock);
        }
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
