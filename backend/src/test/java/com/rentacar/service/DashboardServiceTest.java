package com.rentacar.service;

import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void vehicleCountsByStatus_countsEveryStatusIncludingZero() {
        List<Vehicle> vehicles = List.of(
                Vehicle.builder().id(1L).status(VehicleStatus.AVAILABLE).build(),
                Vehicle.builder().id(2L).status(VehicleStatus.AVAILABLE).build(),
                Vehicle.builder().id(3L).status(VehicleStatus.RENTED).build()
        );
        when(vehicleRepository.findAll()).thenReturn(vehicles);

        var counts = dashboardService.vehicleCountsByStatus();

        assertThat(counts.get("AVAILABLE")).isEqualTo(2L);
        assertThat(counts.get("RENTED")).isEqualTo(1L);
        assertThat(counts.get("RESERVED")).isEqualTo(0L);
        assertThat(counts.get("UNDER_MAINTENANCE")).isEqualTo(0L);
    }

    @Test
    void activeRentals_delegatesToCheckedOutStatus() {
        Reservation checkedOut = Reservation.builder().id(1L).status(ReservationStatus.CHECKED_OUT).build();
        when(reservationRepository.findByStatus(ReservationStatus.CHECKED_OUT)).thenReturn(List.of(checkedOut));

        var result = dashboardService.activeRentals();

        assertThat(result).containsExactly(checkedOut);
    }

    @Test
    void upcomingReservations_queriesPendingAndConfirmedFromToday() {
        Reservation upcoming = Reservation.builder().id(2L).status(ReservationStatus.PENDING)
                .startDate(LocalDate.now().plusDays(3)).build();
        when(reservationRepository.findUpcoming(anyList(), any(LocalDate.class))).thenReturn(List.of(upcoming));

        var result = dashboardService.upcomingReservations();

        assertThat(result).containsExactly(upcoming);
    }
}
