package com.rentacar.dto.category;

import com.rentacar.entity.Category;

import java.math.BigDecimal;

public record CategoryResponse(Long id, String name, BigDecimal dailyRate) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDailyRate());
    }
}
