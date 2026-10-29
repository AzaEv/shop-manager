package com.shopmanager.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.shopmanager.catalog.Category;
import com.shopmanager.catalog.CategoryService;
import com.shopmanager.catalog.Product;
import com.shopmanager.catalog.ProductService;
import com.shopmanager.catalog.ProductSort;

@Controller
public class ShopWebController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ShopWebController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String catalog(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "NAME") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model) {
        boolean ascending = !"desc".equalsIgnoreCase(dir);
        Category selected = categoryId == null ? null : categoryService.get(categoryId);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("products",
                productService.search(q, categoryId, true, ProductSort.from(sort), ascending));
        model.addAttribute("selectedCategory", selected);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("q", q);
        model.addAttribute("sort", ProductSort.from(sort).name());
        model.addAttribute("dir", ascending ? "asc" : "desc");
        model.addAttribute("pageTitle", selected == null ? "Каталог" : selected.getName());
        return "shop/catalog";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id, Model model) {
        Product product = productService.get(id);
        model.addAttribute("product", product);
        model.addAttribute("related", productService.findActive(product.getCategory().getId()).stream()
                .filter(item -> !item.getId().equals(product.getId()))
                .limit(4)
                .toList());
        model.addAttribute("pageTitle", product.getName());
        return "shop/product";
    }
}
