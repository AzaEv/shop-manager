package com.shopmanager.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:pages;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
@ActiveProfiles("test")
class PageRenderTest {

    @LocalServerPort
    int port;

    @Autowired
    CategoryRepository categoryRepository;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void seedNavigation() {
        if (categoryRepository.count() > 0) {
            return;
        }
        String[][] categories = {
                {"Смартфоны", "phones"}, {"Аудио", "audio"}, {"Офис", "office"},
                {"Аксессуары", "accessories"}, {"Для дома", "home"}};
        for (String[] row : categories) {
            Category category = new Category();
            category.setName(row[0]);
            category.setSlug(row[1]);
            categoryRepository.save(category);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/register"})
    void anonymousFormPagesRenderCompletelyWithCsrfToken(String path) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("name=\"_csrf\"").contains("</html>");
        assertThat(response.headers().firstValue("Set-Cookie")).hasValueSatisfying(
                cookie -> assertThat(cookie).startsWith("JSESSIONID="));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/css/app.css", "/js/app.js", "/fonts/manrope-cyrillic.woff2", "/favicon.svg"})
    void staticAssetsArePublic(String path) throws Exception {
        assertThat(get(path).statusCode()).isEqualTo(200);
    }

    @Test
    void catalogRendersForGuest() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("data-search").contains("data-fav-drawer").contains("</html>");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html,*/*")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
