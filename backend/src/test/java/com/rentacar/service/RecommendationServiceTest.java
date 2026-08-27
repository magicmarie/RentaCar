package com.rentacar.service;

import com.rentacar.entity.Category;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private RecommendationService recommendationService;

    private Vehicle economy4Seat;
    private Vehicle suv7Seat;
    private Vehicle luxury5Seat;

    @BeforeEach
    void setUp() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Category suv = Category.builder().id(2L).name("SUV").dailyRate(new BigDecimal("70.00")).build();
        Category luxury = Category.builder().id(3L).name("Luxury").dailyRate(new BigDecimal("120.00")).build();

        economy4Seat = Vehicle.builder().id(1L).seatingCapacity(4).category(economy).status(VehicleStatus.AVAILABLE).build();
        suv7Seat = Vehicle.builder().id(2L).seatingCapacity(7).category(suv).status(VehicleStatus.AVAILABLE).build();
        luxury5Seat = Vehicle.builder().id(3L).seatingCapacity(5).category(luxury).status(VehicleStatus.AVAILABLE).build();
    }

    @Test
    void recommend_filtersByPassengerCount() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), 6, null);

        assertThat(result).containsExactly(suv7Seat);
    }

    @Test
    void recommend_filtersByBudget() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, new BigDecimal("70.00"));

        assertThat(result).containsExactlyInAnyOrder(economy4Seat, suv7Seat);
    }

    @Test
    void recommend_sortsByDailyRateAscending() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(luxury5Seat, economy4Seat, suv7Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, null);

        assertThat(result).containsExactly(economy4Seat, suv7Seat, luxury5Seat);
    }

    @Test
    void recommend_withNoFilters_returnsAllAvailable() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, null);

        assertThat(result).hasSize(3);
    }
}
