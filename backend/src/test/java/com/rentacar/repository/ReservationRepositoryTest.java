package com.rentacar.repository;

import com.rentacar.entity.Category;
import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;

    private Vehicle vehicle;
    private Vehicle otherVehicle;
    private User customer;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(
                Category.builder().name("Economy-" + System.nanoTime()).dailyRate(new BigDecimal("40.00")).build());

        vehicle = vehicleRepository.save(Vehicle.builder()
                .make("Toyota").model("Corolla").year(2022)
                .licensePlate("REPO-" + System.nanoTime()).seatingCapacity(5)
                .category(category).status(VehicleStatus.AVAILABLE).build());

        otherVehicle = vehicleRepository.save(Vehicle.builder()
                .make("Honda").model("Civic").year(2023)
                .licensePlate("REPO2-" + System.nanoTime()).seatingCapacity(5)
                .category(category).status(VehicleStatus.AVAILABLE).build());

        customer = userRepository.save(User.builder()
                .firstName("Jane").lastName("Doe").email("jane-" + System.nanoTime() + "@example.com")
                .passwordHash("hash").role(Role.CUSTOMER).active(true).build());

        reservationRepository.save(Reservation.builder()
                .customer(customer).vehicle(vehicle)
                .startDate(LocalDate.of(2026, 9, 1)).endDate(LocalDate.of(2026, 9, 5))
                .status(ReservationStatus.PENDING).build());
    }

    @Test
    void findOverlapping_findsReservationWithOverlappingDateRange() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).hasSize(1);
    }

    @Test
    void findOverlapping_ignoresNonOverlappingDateRange() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlapping_ignoresDifferentVehicle() {
        var result = reservationRepository.findOverlapping(otherVehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlapping_ignoresStatusesNotInBlockingList() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.CANCELLED));

        assertThat(result).isEmpty();
    }

    @Test
    void findOverlapping_matchesExactBoundaryDates() {
        // requested range ends exactly on the existing reservation's start date
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 1),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).hasSize(1);
    }

    @Test
    void findOverlapping_checkedOutReservationBlocksDatesPastItsPlannedEndDate() {
        // Reservation from setUp() is PENDING with endDate 2026-09-05. A query for
        // 2026-09-10..2026-09-12 doesn't overlap that planned window (confirmed by
        // findOverlapping_ignoresNonOverlappingDateRange above) - but if the same
        // reservation is actually CHECKED_OUT (picked up, not yet returned), its
        // true return date is unknown, so it must still block later dates.
        Reservation reservation = reservationRepository.findByCustomerIdOrderByStartDateDesc(customer.getId()).get(0);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(reservation);

        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).hasSize(1);
    }

    @Test
    void existsByVehicleIdAndStatusIn_trueWhenBlockingReservationExists() {
        boolean exists = reservationRepository.existsByVehicleIdAndStatusIn(vehicle.getId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(exists).isTrue();
    }

    @Test
    void findByCustomerIdOrderByStartDateDesc_returnsOnlyThatCustomersReservations() {
        var result = reservationRepository.findByCustomerIdOrderByStartDateDesc(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomer().getId()).isEqualTo(customer.getId());
    }
}
