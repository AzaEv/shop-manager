package com.shopmanager.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CatalogDtos {

    public record CategoryRequest(
            @NotBlank String name,
            @NotBlank String slug) {
    }

    public record CategoryResponse(Long id, String name, String slug) {
    }

    public record ProductRequest(
            @NotNull Long categoryId,
            @NotBlank String name,
            @NotBlank String sku,
            String description,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Min(0) int stock,
            boolean active) {
    }

    public record ProductResponse(
            Long id,
            Long categoryId,
            String categoryName,
            String name,
            String sku,
            String description,
            BigDecimal price,
            int stock,
            boolean active) {
    }
}
