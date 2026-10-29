package com.shopmanager.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shopmanager.cart.Cart;
import com.shopmanager.cart.CartService;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;
import com.shopmanager.config.CurrentUser;
import com.shopmanager.customer.Customer;
import com.shopmanager.customer.CustomerService;
import com.shopmanager.order.OrderService;
import com.shopmanager.order.OrderStatus;
import com.shopmanager.order.ReceiptPdfService;
import com.shopmanager.order.ShopOrder;

@Controller
public class OrderWebController {

    private final OrderService orderService;
    private final CartService cartService;
    private final CustomerService customerService;
    private final CurrentUser currentUser;
    private final ReceiptPdfService receiptPdfService;

    public OrderWebController(OrderService orderService, CartService cartService, CustomerService customerService,
            CurrentUser currentUser, ReceiptPdfService receiptPdfService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.customerService = customerService;
        this.currentUser = currentUser;
        this.receiptPdfService = receiptPdfService;
    }

    @GetMapping("/checkout")
    public String checkoutForm(Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        Cart cart = cartService.getFor(customer);
        model.addAttribute("cart", cart);
        model.addAttribute("total", cartService.total(cart));
        model.addAttribute("pageTitle", "Оформление заказа");
        return "shop/checkout";
    }

    @PostMapping("/checkout")
    public String checkout(Authentication authentication, @RequestParam String address,
            @RequestParam(required = false) String saveAddress,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Customer customer = currentUser.require(authentication);
        String trimmed = address == null ? "" : address.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException("Укажите адрес доставки");
        }
        ShopOrder order = orderService.checkout(customer, trimmed);
        if (saveAddress != null) {
            customerService.saveAddress(customer.getId(), trimmed);
        }
        redirectAttributes.addFlashAttribute("message", "Заказ оформлен. Осталось оплатить.");
        return "redirect:/orders/" + order.getId() + "/pay";
    }

    @GetMapping("/orders/{id}/pay")
    public String payForm(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = visibleOrder(customer, id);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Заказ не найден: " + id);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Отменённый заказ нельзя оплатить");
        }
        if (order.getStatus() != OrderStatus.NEW) {
            return "redirect:/orders/" + id + "/receipt";
        }
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "Оплата заказа");
        return "shop/pay";
    }

    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable Long id, Authentication authentication, @RequestParam String card,
            @RequestParam String expiry, @RequestParam String cvc,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = visibleOrder(customer, id);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Заказ не найден: " + id);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Отменённый заказ нельзя оплатить");
        }
        if (order.getStatus() != OrderStatus.NEW) {
            return "redirect:/orders/" + id + "/receipt";
        }
        requireSimulatedCard(card, expiry, cvc);
        orderService.pay(customer, id);
        redirectAttributes.addFlashAttribute("message", "Оплата прошла. Чек готов.");
        return "redirect:/orders/" + id + "/receipt";
    }

    @GetMapping("/orders/{id}/receipt")
    public String receipt(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = receiptOrder(customer, id);
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "Чек № " + order.getId());
        return "shop/receipt";
    }

    @GetMapping("/orders/{id}/receipt.pdf")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id, Authentication authentication) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = receiptOrder(customer, id);
        byte[] pdf = receiptPdfService.render(order);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nova-chek-" + order.getId() + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/orders")
    public String myOrders(Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        model.addAttribute("orders", orderService.listForCustomer(customer));
        model.addAttribute("pageTitle", "Мои заказы");
        return "shop/orders";
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Authentication authentication, Model model) {
        Customer customer = currentUser.require(authentication);
        ShopOrder order = orderService.get(id);
        if (!customer.getRole().managesOrders() && !order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Заказ не найден: " + id);
        }
        model.addAttribute("order", order);
        model.addAttribute("pageTitle", "Заказ №" + order.getId());
        return "shop/order";
    }

    private ShopOrder visibleOrder(Customer customer, Long id) {
        ShopOrder order = orderService.get(id);
        if (!customer.getRole().managesOrders() && !order.getCustomer().getId().equals(customer.getId())) {
            throw new NotFoundException("Заказ не найден: " + id);
        }
        return order;
    }

    private ShopOrder receiptOrder(Customer customer, Long id) {
        ShopOrder order = visibleOrder(customer, id);
        if (order.getStatus() == OrderStatus.NEW) {
            throw new BusinessException("Сначала оплатите заказ");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("По отменённому заказу чек не выдаётся");
        }
        return order;
    }

    private static void requireSimulatedCard(String card, String expiry, String cvc) {
        String digits = card == null ? "" : card.replaceAll("\\D", "");
        if (digits.length() < 16 || digits.length() > 19) {
            throw new BusinessException("Укажите номер карты: 16 цифр");
        }
        if (expiry == null || !expiry.trim().matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new BusinessException("Укажите срок карты в формате ММ/ГГ");
        }
        if (cvc == null || !cvc.trim().matches("\\d{3}")) {
            throw new BusinessException("Укажите код из 3 цифр");
        }
    }
}
