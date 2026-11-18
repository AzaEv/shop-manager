package com.shopmanager;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryRepository;
import com.shopmanager.catalog.ProductRepository;
import com.shopmanager.catalog.ProductService;
import com.shopmanager.common.BusinessException;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.customer.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopScenarioTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductService productService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerService customerService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        if (categoryRepository.count() == 0) {
            Category phones = new Category();
            phones.setName("Смартфоны");
            phones.setSlug("phones");
            categoryRepository.save(phones);
            productService.create(phones.getId(), "Nova X15", "PHN-X15", "Смартфон", new BigDecimal("34990.00"), 12,
                    true);
            productService.create(phones.getId(), "Wave Buds", "AUD-WB1", "Наушники", new BigDecimal("5990.00"), 30,
                    true);
            customerService.register("admin@shop.local", "Админ", "+7000", "admin123", Role.ADMIN);
            customerService.register("customer@shop.local", "Покупатель", "+7001", "customer123", Role.CUSTOMER);
        }
    }

    @Test
    void t01UnauthorizedCartIsRejected() throws Exception {
        mockMvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void t02CustomerCannotCreateProduct() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":1,"name":"X","sku":"X-1","price":10,"stock":1,"active":true}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void t03AdminCreatesProduct() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":1,"name":"Desk Lamp","sku":"OFF-DL2","description":"Лампа","price":2490.00,"stock":8,"active":true}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("OFF-DL2"));
    }

    @Test
    void t04ViewCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void t05SearchByName() throws Exception {
        mockMvc.perform(get("/api/products").param("q", "nova"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", containsString("Nova")));
    }

    @Test
    void t06SortByPriceDesc() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "PRICE").param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price", greaterThanOrEqualTo(10000.0)));
    }

    @Test
    void t07AdminUpdatesProduct() throws Exception {
        mockMvc.perform(put("/api/admin/products/1")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":1,"name":"Nova X15","sku":"PHN-X15","description":"Обновлено","price":33990.00,"stock":11,"active":true}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Обновлено"));
    }

    @Test
    void t08DuplicateSkuIsConflict() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":1,"name":"Clone","sku":"PHN-X15","price":10,"stock":1,"active":true}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void t09CheckoutAndReport() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/orders")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"Москва, Тверская 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        long orderId = order.get("id").asLong();

        mockMvc.perform(post("/api/orders/" + orderId + "/status")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(get("/api/admin/report").with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.topProducts", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void t10InvalidStatusIsConflict() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":2,\"quantity\":1}"))
                .andExpect(status().isOk());
        MvcResult result = mockMvc.perform(post("/api/orders")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"СПб\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post("/api/orders/" + orderId + "/status")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void t11DeleteProductInOrderIsForbiddenByBusinessRule() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":2,\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"Казань\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/admin/products/2").with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isConflict());
    }

    @Test
    void t12ValidationErrorOnEmptySku() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                .with(httpBasic("admin@shop.local", "admin123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryId":1,"name":"Bad","sku":"","price":10,"stock":1,"active":true}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void t13StaffCannotUseCart() throws Exception {
        mockMvc.perform(get("/api/cart").with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void t14OnlyAdminDownloadsReport() throws Exception {
        mockMvc.perform(get("/admin/report.csv").with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/report.csv").with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Показатель;Значение")))
                .andExpect(content().string(containsString("Выручка, руб.")))
                .andExpect(content().string(containsString("Статус;Заказов")));
        MvcResult ods = mockMvc.perform(get("/admin/report.ods").with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.oasis.opendocument.spreadsheet"))
                .andReturn();
        mockMvc.perform(get("/admin/report.ods").with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isForbidden());
        String sheet = unzip(ods.getResponse().getContentAsByteArray(), "content.xml");
        assertTrue(sheet.contains("table:name=\"Сводка\""));
        assertTrue(sheet.contains("table:name=\"Заказы\""));
        assertTrue(sheet.contains("Отчёт о продажах nova"));
        assertTrue(sheet.contains("Выручка"));
    }

    private static String unzip(byte[] archive, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(archive))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    @Test
    void t15StorefrontRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nova")))
                .andExpect(content().string(containsString("Nova для каждого дня")))
                .andExpect(content().string(containsString("Nova X15")));
        mockMvc.perform(get("/").param("q", "Wave"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Wave Buds")));
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Войти, чтобы купить")));
        mockMvc.perform(get("/products/1").with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("В корзину")));
    }

    @Test
    void t16CustomerEditsProfile() throws Exception {
        String email = "profile-" + UUID.randomUUID() + "@shop.local";
        Customer created = customerService.register(email, "Старое имя", "+7002", "secret12", Role.CUSTOMER);

        mockMvc.perform(post("/profile")
                        .with(httpBasic(email, "secret12"))
                        .with(csrf())
                        .param("name", "Новое имя")
                        .param("email", email)
                        .param("phone", "+79990001122")
                        .param("address", "Душанбе, пр. Рудаки 1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        mockMvc.perform(get("/profile").with(httpBasic(email, "secret12")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Новое имя")))
                .andExpect(content().string(containsString("Душанбе, пр. Рудаки 1")));

        assertThrows(BusinessException.class, () -> customerService.updateProfile(
                created.getId(), "Новое имя", email, "+79990001122", "Душанбе", "wrong-pass", "newpass1"));

        Customer renamed = customerService.updateProfile(
                created.getId(), "Новое имя", "moved-" + email, "+79990001122", "Душанбе", "secret12", "newpass1");
        assertEquals("moved-" + email, renamed.getEmail());
        assertTrue(passwordEncoder.matches("newpass1", renamed.getPasswordHash()));

        mockMvc.perform(get("/profile").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void t17PaymentSimulationReturnsPdfReceipt() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                .with(httpBasic("customer@shop.local", "customer123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":1,\"quantity\":1}"))
                .andExpect(status().isOk());

        MvcResult checkout = mockMvc.perform(post("/checkout")
                        .with(httpBasic("customer@shop.local", "customer123"))
                        .with(csrf())
                        .param("address", "Ташкент, ул. Бабура, 22"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*/pay"))
                .andReturn();

        String payUrl = checkout.getResponse().getRedirectedUrl();
        mockMvc.perform(get(payUrl).with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Оплатить")))
                .andExpect(content().string(containsString("симуляция")));

        MvcResult paid = mockMvc.perform(post(payUrl)
                        .with(httpBasic("customer@shop.local", "customer123"))
                        .with(csrf())
                        .param("card", "4242 4242 4242 4242")
                        .param("expiry", "12/28")
                        .param("cvc", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/orders/*/receipt"))
                .andReturn();

        String receiptUrl = paid.getResponse().getRedirectedUrl();
        mockMvc.perform(get(receiptUrl).with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Печать магазина")))
                .andExpect(content().string(containsString("Скачать чек PDF")))
                .andExpect(content().string(not(containsString("<iframe"))));

        mockMvc.perform(get(receiptUrl + ".pdf").with(httpBasic("customer@shop.local", "customer123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().string(containsString("%PDF")));

        mockMvc.perform(get("/api/admin/report")
                        .param("group", "model")
                        .param("sort", "amount")
                        .param("dir", "desc")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdown[0].label").value("Nova X15"));

        mockMvc.perform(get("/admin/report")
                        .param("group", "day")
                        .param("sort", "amount")
                        .param("dir", "asc")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Сравнение выручки")))
                .andExpect(content().string(containsString("По дням")))
                .andExpect(content().string(containsString("is-sorted")));

        mockMvc.perform(get("/admin/report.csv")
                        .param("group", "category")
                        .param("sort", "quantity")
                        .param("dir", "desc")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Категория;Заказов;Продано, шт.;Выручка, руб.")))
                .andExpect(content().string(containsString("Смартфоны")));
    }

    @Test
    void t18ReportSortsModelsByRevenue() throws Exception {
        String email = "report-" + UUID.randomUUID() + "@shop.local";
        customerService.register(email, "Отчёт", "+7009", "secret12", Role.CUSTOMER);
        long nova = productRepository.findBySku("PHN-X15").orElseThrow().getId();
        long buds = productRepository.findBySku("AUD-WB1").orElseThrow().getId();

        mockMvc.perform(post("/api/cart/items")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + nova + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cart/items")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + buds + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"Самарканд, Регистан 1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/report")
                        .param("group", "model")
                        .param("sort", "amount")
                        .param("dir", "asc")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdown[0].label").value("Wave Buds"))
                .andExpect(jsonPath("$.breakdown[1].label").value("Nova X15"));

        mockMvc.perform(get("/api/admin/report")
                        .param("group", "category")
                        .param("sort", "amount")
                        .param("dir", "desc")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdown[0].label").value("Смартфоны"));
    }

    @Test
    void t19ReportFiltersByPeriodStatusCategoryAndTop() throws Exception {
        String email = "filter-" + UUID.randomUUID() + "@shop.local";
        customerService.register(email, "Фильтр", "+7010", "secret12", Role.CUSTOMER);
        var nova = productRepository.findBySku("PHN-X15").orElseThrow();
        long buds = productRepository.findBySku("AUD-WB1").orElseThrow().getId();
        long phones = nova.getCategory().getId();
        String phonesName = categoryRepository.findById(phones).orElseThrow().getName();

        mockMvc.perform(post("/api/cart/items")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + nova.getId() + ",\"quantity\":1}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/cart/items")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + buds + ",\"quantity\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orders")
                        .with(httpBasic(email, "secret12"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\":\"Бухара, Ляби-хауз 3\"}"))
                .andExpect(status().isCreated());

        String today = LocalDate.now(ZoneId.of("Asia/Tashkent")).toString();

        mockMvc.perform(get("/api/admin/report")
                        .param("from", "2000-01-01")
                        .param("to", "2000-01-31")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount").value(0))
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.breakdown", hasSize(31)))
                .andExpect(jsonPath("$.period").value("Период с 01.01.2000 по 31.01.2000"));

        mockMvc.perform(get("/api/admin/report")
                        .param("from", today)
                        .param("to", today)
                        .param("group", "category")
                        .param("category", Long.toString(phones))
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.breakdown", hasSize(1)))
                .andExpect(jsonPath("$.breakdown[0].label").value(phonesName))
                .andExpect(jsonPath("$.breakdown[0].percentOfRevenue").value(100));

        mockMvc.perform(get("/api/admin/report")
                        .param("status", "CANCELLED")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(0))
                .andExpect(jsonPath("$.byStatus", hasSize(1)))
                .andExpect(jsonPath("$.byStatus[0].status").value("CANCELLED"));

        mockMvc.perform(get("/api/admin/report")
                        .param("top", "5")
                        .param("from", "не-дата")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topProducts", hasSize(lessThanOrEqualTo(5))));

        mockMvc.perform(get("/admin/report")
                        .param("from", today)
                        .param("to", today)
                        .param("status", "NEW")
                        .param("category", Long.toString(phones))
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Показать")))
                .andExpect(content().string(containsString("категория «" + phonesName + "»")))
                .andExpect(content().string(containsString("status=NEW")));

        mockMvc.perform(get("/admin/report.csv")
                        .param("from", "2000-01-01")
                        .param("to", "2000-01-02")
                        .with(httpBasic("admin@shop.local", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Период с 01.01.2000 по 02.01.2000")));
    }
}
