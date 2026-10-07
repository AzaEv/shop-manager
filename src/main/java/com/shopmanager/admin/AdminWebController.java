package com.shopmanager.admin;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shopmanager.catalog.CategoryService;
import com.shopmanager.catalog.ProductService;
import com.shopmanager.catalog.ProductSort;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.order.OrderService;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.report.ReportService;

@Controller
@RequestMapping("/admin")
public class AdminWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CustomerService customerService;
    private final OrderService orderService;
    private final ReportService reportService;

    public AdminWebController(ProductService productService, CategoryService categoryService,
            CustomerService customerService, OrderService orderService, ReportService reportService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.customerService = customerService;
        this.orderService = orderService;
        this.reportService = reportService;
    }

    @GetMapping
    public String home() {
        return "redirect:/admin/report";
    }

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("report", reportService.build());
        model.addAttribute("pageTitle", "Аналитический отчёт");
        return "admin/report";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "NAME") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model) {
        boolean ascending = !"desc".equalsIgnoreCase(dir);
        model.addAttribute("products", productService.search(q, categoryId, null, ProductSort.from(sort), ascending));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("q", q);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sort", ProductSort.from(sort).name());
        model.addAttribute("dir", ascending ? "asc" : "desc");
        model.addAttribute("pageTitle", "Товары");
        return "admin/products";
    }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "Новый товар");
        return "admin/product-form";
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.get(id));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageTitle", "Редактирование товара");
        return "admin/product-form";
    }

    @PostMapping("/products")
    public String createProduct(
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam String sku,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam int stock,
            @RequestParam(defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {
        productService.create(categoryId, name, sku, description, price, stock, active);
        redirectAttributes.addFlashAttribute("message", "Товар создан");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @RequestParam Long categoryId,
            @RequestParam String name,
            @RequestParam String sku,
            @RequestParam(required = false) String description,
            @RequestParam BigDecimal price,
            @RequestParam int stock,
            @RequestParam(defaultValue = "false") boolean active,
            RedirectAttributes redirectAttributes) {
        productService.update(id, categoryId, name, sku, description, price, stock, active);
        redirectAttributes.addFlashAttribute("message", "Товар сохранён");
        return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Товар удалён");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories")
    public String createCategory(@RequestParam String name, @RequestParam String slug,
            RedirectAttributes redirectAttributes) {
        categoryService.create(name, slug);
        redirectAttributes.addFlashAttribute("message", "Категория создана");
        return "redirect:/admin/products";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Категория удалена");
        return "redirect:/admin/products";
    }

    @GetMapping("/customers")
    public String customers(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("customers", customerService.search(q));
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Клиенты");
        return "admin/customers";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, Model model) {
        model.addAttribute("orders", orderService.listAll(status));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Заказы");
        return "admin/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.get(id));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pageTitle", "Заказ №" + id);
        return "admin/order";
    }

    @PostMapping("/orders/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam OrderStatus status,
            RedirectAttributes redirectAttributes) {
        orderService.changeStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Статус обновлён");
        return "redirect:/admin/orders/" + id;
    }
}
