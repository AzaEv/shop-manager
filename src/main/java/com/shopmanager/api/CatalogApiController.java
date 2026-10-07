package com.shopmanager.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shopmanager.api.dto.CatalogDtos.CategoryRequest;
import com.shopmanager.api.dto.CatalogDtos.CategoryResponse;
import com.shopmanager.api.dto.CatalogDtos.ProductRequest;
import com.shopmanager.api.dto.CatalogDtos.ProductResponse;
import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryService;
import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductService;
import com.shopmanager.catalog.ProductSort;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CatalogApiController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public CatalogApiController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.findAll().stream().map(this::toCategory).toList();
    }

    @PostMapping("/admin/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return toCategory(categoryService.create(request.name(), request.slug()));
    }

    @PutMapping("/admin/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return toCategory(categoryService.update(id, request.name(), request.slug()));
    }

    @DeleteMapping("/admin/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        categoryService.delete(id);
    }

    @GetMapping("/products")
    public List<ProductResponse> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "NAME") String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir) {
        return productService.search(q, categoryId, true, ProductSort.from(sort), !"desc".equalsIgnoreCase(dir))
                .stream()
                .map(this::toProduct)
                .toList();
    }

    @GetMapping("/admin/products")
    public List<ProductResponse> adminProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "NAME") String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir) {
        return productService.search(q, categoryId, null, ProductSort.from(sort), !"desc".equalsIgnoreCase(dir))
                .stream()
                .map(this::toProduct)
                .toList();
    }

    @GetMapping("/products/{id}")
    public ProductResponse product(@PathVariable Long id) {
        return toProduct(productService.get(id));
    }

    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        return toProduct(productService.create(
                request.categoryId(),
                request.name(),
                request.sku(),
                request.description(),
                request.price(),
                request.stock(),
                request.active()));
    }

    @PutMapping("/admin/products/{id}")
    public ProductResponse updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return toProduct(productService.update(
                id,
                request.categoryId(),
                request.name(),
                request.sku(),
                request.description(),
                request.price(),
                request.stock(),
                request.active()));
    }

    @DeleteMapping("/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.delete(id);
    }

    private CategoryResponse toCategory(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }

    private ProductResponse toProduct(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.isActive());
    }
}
