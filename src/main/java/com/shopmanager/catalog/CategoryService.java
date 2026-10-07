package com.shopmanager.catalog;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopmanager.common.BusinessException;
import com.shopmanager.common.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория не найдена: " + id));
    }

    @Transactional
    public Category create(String name, String slug) {
        String normalized = normalizeSlug(slug);
        if (categoryRepository.existsBySlug(normalized)) {
            throw new BusinessException("Категория со slug уже существует: " + normalized);
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(normalized);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, String name, String slug) {
        Category category = get(id);
        String normalized = normalizeSlug(slug);
        categoryRepository.findBySlug(normalized)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException("Категория со slug уже существует: " + normalized);
                });
        category.setName(name);
        category.setSlug(normalized);
        return category;
    }

    @Transactional
    public void delete(Long id) {
        Category category = get(id);
        if (productRepository.countByCategoryId(id) > 0) {
            throw new BusinessException("Нельзя удалить категорию «" + category.getName()
                    + "»: в ней есть товары");
        }
        categoryRepository.delete(category);
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }
}
