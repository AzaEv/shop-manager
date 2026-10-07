package com.shopmanager;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

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
import com.shopmanager.catalog.ProductService;
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
    CustomerService customerService;

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
}
