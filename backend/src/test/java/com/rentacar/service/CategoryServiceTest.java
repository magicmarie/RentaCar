package com.rentacar.service;

import com.rentacar.dto.category.CategoryRateUpdateRequest;
import com.rentacar.dto.category.CategoryRequest;
import com.rentacar.entity.Category;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.CategoryRepository;
import com.rentacar.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    // @Mock stubs the repositories so category logic is tested without a real database.
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private VehicleRepository vehicleRepository;

    // @InjectMocks builds a real CategoryService wired with the mocks above.
    @InjectMocks
    private CategoryService categoryService;

    // Verifies category names must be unique (case-insensitively), so two
    // "Economy" categories can't be created by accident.
    @Test
    void create_rejectsDuplicateName() {
        var request = new CategoryRequest("Economy", new BigDecimal("40.00"));
        when(categoryRepository.existsByNameIgnoreCase("Economy")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    // Verifies a category with a unique name is created and persisted as given.
    @Test
    void create_savesNewCategory() {
        var request = new CategoryRequest("Economy", new BigDecimal("40.00"));
        when(categoryRepository.existsByNameIgnoreCase("Economy")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.create(request);

        assertThat(result.getName()).isEqualTo("Economy");
        assertThat(result.getDailyRate()).isEqualByComparingTo("40.00");
    }

    // Verifies updating a category's rate leaves other fields (like name) untouched.
    @Test
    void updateRate_changesOnlyDailyRate() {
        Category category = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        Category result = categoryService.updateRate(1L, new CategoryRateUpdateRequest(new BigDecimal("55.00")));

        assertThat(result.getName()).isEqualTo("Economy");
        assertThat(result.getDailyRate()).isEqualByComparingTo("55.00");
    }

    // Verifies a category can't be deleted while vehicles still reference it,
    // which would otherwise orphan those vehicles' pricing.
    @Test
    void delete_rejectsWhenVehiclesAssigned() {
        Category category = Category.builder().id(1L).name("Economy").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(vehicleRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(InvalidStateException.class);

        verify(categoryRepository, never()).delete(any());
    }

    // Verifies deletion is allowed once no vehicles are assigned to the category.
    @Test
    void delete_succeedsWhenNoVehiclesAssigned() {
        Category category = Category.builder().id(1L).name("Economy").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(vehicleRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(category);
    }

    // Verifies looking up a nonexistent category id fails clearly instead of
    // returning null and causing a NullPointerException downstream.
    @Test
    void getById_throwsWhenMissing() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
