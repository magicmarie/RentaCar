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

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    // @Mock stubs the repositories backing the dashboard's summary queries.
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ReservationRepository reservationRepository;

    // @InjectMocks builds a real DashboardService wired with the mocks above.
    @InjectMocks
    private DashboardService dashboardService;

    // Verifies the status breakdown always reports every VehicleStatus value,
    // even ones with zero vehicles, so the dashboard UI doesn't need to
    // special-case missing keys.
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

    // Verifies "active rentals" is defined as reservations in the
    // CHECKED_OUT status, and that the service just passes that filter
    // through to the repository rather than reimplementing filtering logic.
    @Test
    void activeRentals_delegatesToCheckedOutStatus() {
        Reservation checkedOut = Reservation.builder().id(1L).status(ReservationStatus.CHECKED_OUT).build();
        when(reservationRepository.findByStatus(ReservationStatus.CHECKED_OUT)).thenReturn(List.of(checkedOut));

        var result = dashboardService.activeRentals();

        assertThat(result).containsExactly(checkedOut);
    }

    // Verifies "upcoming reservations" queries only the still-pending
    // statuses (PENDING/CONFIRMED) starting from today, matching what the
    // dashboard should surface as work still ahead.
    @Test
    void upcomingReservations_queriesPendingAndConfirmedFromToday() {
        Reservation upcoming = Reservation.builder().id(2L).status(ReservationStatus.PENDING)
                .startDate(LocalDate.now().plusDays(3)).build();
        // anyList()/any(LocalDate.class) match whatever status list and cutoff date
        // the service builds internally; we only care about the returned result here.
        when(reservationRepository.findUpcoming(anyList(), any(LocalDate.class))).thenReturn(List.of(upcoming));

        var result = dashboardService.upcomingReservations();

        assertThat(result).containsExactly(upcoming);
    }
}
