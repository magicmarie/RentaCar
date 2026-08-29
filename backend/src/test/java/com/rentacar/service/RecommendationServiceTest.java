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

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    // @Mock: RecommendationService delegates availability lookups to
    // ReservationService, so we stub that boundary rather than the database.
    @Mock
    private ReservationService reservationService;

    // @InjectMocks builds a real RecommendationService wired with the mock above.
    @InjectMocks
    private RecommendationService recommendationService;

    private Vehicle economy4Seat;
    private Vehicle suv7Seat;
    private Vehicle luxury5Seat;

    // Shared fixture: three vehicles spanning different seating capacities
    // and price points, reused across tests to check filtering/sorting.
    @BeforeEach
    void setUp() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Category suv = Category.builder().id(2L).name("SUV").dailyRate(new BigDecimal("70.00")).build();
        Category luxury = Category.builder().id(3L).name("Luxury").dailyRate(new BigDecimal("120.00")).build();

        economy4Seat = Vehicle.builder().id(1L).seatingCapacity(4).category(economy).status(VehicleStatus.AVAILABLE).build();
        suv7Seat = Vehicle.builder().id(2L).seatingCapacity(7).category(suv).status(VehicleStatus.AVAILABLE).build();
        luxury5Seat = Vehicle.builder().id(3L).seatingCapacity(5).category(luxury).status(VehicleStatus.AVAILABLE).build();
    }

    // Verifies vehicles seating fewer passengers than requested are excluded,
    // even if otherwise available.
    @Test
    void recommend_filtersByPassengerCount() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), 6, null);

        assertThat(result).containsExactly(suv7Seat);
    }

    // Verifies vehicles priced above the given daily-rate budget are excluded.
    @Test
    void recommend_filtersByBudget() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, new BigDecimal("70.00"));

        assertThat(result).containsExactlyInAnyOrder(economy4Seat, suv7Seat);
    }

    // Verifies results are ordered cheapest-first regardless of the order
    // returned by the underlying availability search.
    @Test
    void recommend_sortsByDailyRateAscending() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(luxury5Seat, economy4Seat, suv7Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, null);

        assertThat(result).containsExactly(economy4Seat, suv7Seat, luxury5Seat);
    }

    // Verifies that with no seating/budget filters applied, every available
    // vehicle is returned (i.e. filters are optional, not silently required).
    @Test
    void recommend_withNoFilters_returnsAllAvailable() {
        when(reservationService.searchAvailability(any(), any(), isNull()))
                .thenReturn(List.of(economy4Seat, suv7Seat, luxury5Seat));

        var result = recommendationService.recommend(LocalDate.now(), LocalDate.now().plusDays(2), null, null);

        assertThat(result).hasSize(3);
    }
}
