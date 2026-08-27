package com.rentacar.service;

import com.rentacar.dto.category.CategoryRateUpdateRequest;
import com.rentacar.dto.category.CategoryRequest;
import com.rentacar.entity.Category;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.CategoryRepository;
import com.rentacar.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public Category create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }

        Category category = Category.builder()
                .name(request.name())
                .dailyRate(request.dailyRate())
                .build();

        return categoryRepository.save(category);
    }

    public List<Category> list() {
        return categoryRepository.findAll();
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    @Transactional
    public Category updateRate(Long id, CategoryRateUpdateRequest request) {
        Category category = getById(id);
        category.setDailyRate(request.dailyRate());
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getById(id);

        if (vehicleRepository.existsByCategoryId(id)) {
            throw new InvalidStateException("This category is still assigned to some vehicles");
        }

        categoryRepository.delete(category);
    }
}
