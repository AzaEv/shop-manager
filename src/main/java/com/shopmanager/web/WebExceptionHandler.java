package com.shopmanager.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.shopmanager.admin.AdminWebController;
import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(assignableTypes = {
        ShopWebController.class,
        AuthWebController.class,
        CartWebController.class,
        OrderWebController.class,
        AdminWebController.class
})
public class WebExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String business(BusinessException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }

    @ExceptionHandler(NotFoundException.class)
    public String notFound(NotFoundException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String missingStatic() {
        return "redirect:/";
    }
}
