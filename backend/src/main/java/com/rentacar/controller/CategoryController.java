package com.rentacar.controller;

import com.rentacar.dto.category.CategoryRateUpdateRequest;
import com.rentacar.dto.category.CategoryRequest;
import com.rentacar.dto.category.CategoryResponse;
import com.rentacar.dto.common.MessageResponse;
import com.rentacar.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.list().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return CategoryResponse.from(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(CategoryResponse.from(categoryService.create(request)));
    }

    @PutMapping("/{id}/rate")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateRate(@PathVariable Long id, @Valid @RequestBody CategoryRateUpdateRequest request) {
        return CategoryResponse.from(categoryService.updateRate(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MessageResponse delete(@PathVariable Long id) {
        categoryService.delete(id);
        return new MessageResponse("Category deleted successfully");
    }
}
