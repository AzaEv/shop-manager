package com.shopmanager.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryRepository;
import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;
import com.shopmanager.order.OrderItem;
import com.shopmanager.order.OrderRepository;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.order.OrderStatusHistory;
import com.shopmanager.order.ShopOrder;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerService customerService;
    private final OrderRepository orderRepository;

    public DataSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
            CustomerService customerService, OrderRepository orderRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerService = customerService;
        this.orderRepository = orderRepository;
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

        ensureUser("admin@shop.local", "Администратор", "+70000000000", "admin123", Role.ADMIN, null);
        ensureUser("manager@shop.local", "Мария Менеджер", "+70000000002", "manager123", Role.MANAGER, null);
        ensureUser("warehouse@shop.local", "Пётр Кладовщик", "+70000000003", "warehouse123", Role.WAREHOUSE, null);
        Customer ivan = ensureUser("customer@shop.local", "Иван Покупатель", "+70000000001", "customer123",
                Role.CUSTOMER, "Ташкент, ул. Бабура, 22");
        Customer anna = ensureUser("anna@shop.local", "Анна Соколова", "+998901110011", "customer123", Role.CUSTOMER,
                "Ташкент, ул. Амира Темура, 18");
        Customer dmitry = ensureUser("dmitry@shop.local", "Дмитрий Орлов", "+998901110022", "customer123",
                Role.CUSTOMER, "Ташкент, массив Чиланзар, 9");
        Customer elena = ensureUser("elena@shop.local", "Елена Ким", "+998901110033", "customer123", Role.CUSTOMER,
                "Самарканд, ул. Регистан, 4");
        Customer oleg = ensureUser("oleg@shop.local", "Олег Назаров", "+998901110044", "customer123", Role.CUSTOMER,
                "Бухара, ул. Накшбанди, 7");
        Customer marina = ensureUser("marina@shop.local", "Марина Юсупова", "+998901110055", "customer123",
                Role.CUSTOMER, "Ташкент, ул. Навои, 41");
        Customer sergey = ensureUser("sergey@shop.local", "Сергей Алиев", "+998901110066", "customer123",
                Role.CUSTOMER, "Ташкент, Юнусабад, 15");
        Customer kate = ensureUser("kate@shop.local", "Екатерина Волкова", "+998901110077", "customer123",
                Role.CUSTOMER, "Ташкент, ул. Шота Руставели, 3");

        if (orderRepository.count() == 0) {
            place(ivan, OrderStatus.DELIVERED, ivan.getAddress(), daysAgo(20), "PHN-MINI", 1);
            place(anna, OrderStatus.DELIVERED, anna.getAddress(), daysAgo(14), "PHN-X15", 1, "ACC-USBC", 2);
            place(anna, OrderStatus.PAID, anna.getAddress(), daysAgo(1), "OFF-MP1", 2);
            place(dmitry, OrderStatus.DELIVERED, dmitry.getAddress(), daysAgo(11), "AUD-WB1", 1, "HOM-MUG", 2);
            place(dmitry, OrderStatus.SHIPPED, dmitry.getAddress(), daysAgo(2), "AUD-WS1", 1, "AUD-MIC", 1);
            place(elena, OrderStatus.PROCESSING, elena.getAddress(), daysAgo(3), "AUD-WH1", 1);
            place(elena, OrderStatus.CANCELLED, elena.getAddress(), daysAgo(5), "OFF-CH1", 1);
            place(oleg, OrderStatus.SHIPPED, oleg.getAddress(), daysAgo(4), "PHN-AIR", 1, "ACC-CS1", 1);
            place(marina, OrderStatus.PAID, marina.getAddress(), daysAgo(1), "ACC-PB1", 2, "ACC-USBC", 1);
            place(sergey, OrderStatus.NEW, sergey.getAddress(), daysAgo(0), "HOM-PLT", 1, "OFF-PEN", 1);
            place(kate, OrderStatus.DELIVERED, kate.getAddress(), daysAgo(8), "OFF-DL2", 1, "OFF-NB5", 3, "HOM-MUG", 1);
        }
    }

    private Customer ensureUser(String email, String name, String phone, String password, Role role, String address) {
        Customer customer = customerService.findOptional(email)
                .orElseGet(() -> customerService.register(email, name, phone, password, role));
        if (address != null && (customer.getAddress() == null || customer.getAddress().isBlank())) {
            customerService.saveAddress(customer.getId(), address);
            customer.setAddress(address);
        }
        return customer;
    }

    private void place(Customer customer, OrderStatus status, String address, Instant created, Object... lines) {
        ShopOrder order = new ShopOrder();
        order.setCustomer(customer);
        order.setAddress(address == null ? "Самовывоз" : address);
        order.setStatus(status);
        order.setCreatedAt(created);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < lines.length; i += 2) {
            Product product = productRepository.findBySku(String.valueOf(lines[i])).orElseThrow();
            int quantity = Integer.parseInt(String.valueOf(lines[i + 1]));
            if (status != OrderStatus.CANCELLED) {
                product.setStock(Math.max(0, product.getStock() - quantity));
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setSku(product.getSku());
            item.setUnitPrice(product.getPrice());
            item.setQuantity(quantity);
            order.getItems().add(item);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);
        Instant moment = created;
        OrderStatus previous = null;
        for (OrderStatus step : path(status)) {
            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setFromStatus(previous);
            history.setToStatus(step);
            history.setChangedAt(moment);
            order.getHistory().add(history);
            previous = step;
            moment = moment.plus(6, ChronoUnit.HOURS);
        }
        orderRepository.save(order);
    }

    private static List<OrderStatus> path(OrderStatus status) {
        return switch (status) {
            case NEW -> List.of(OrderStatus.NEW);
            case PAID -> List.of(OrderStatus.NEW, OrderStatus.PAID);
            case PROCESSING -> List.of(OrderStatus.NEW, OrderStatus.PAID, OrderStatus.PROCESSING);
            case SHIPPED -> List.of(OrderStatus.NEW, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED);
            case DELIVERED -> List.of(OrderStatus.NEW, OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED,
                    OrderStatus.DELIVERED);
            case CANCELLED -> List.of(OrderStatus.NEW, OrderStatus.CANCELLED);
        };
    }

    private static Instant daysAgo(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS);
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
