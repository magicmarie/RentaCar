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

// @DataJpaTest configures an in-memory-style test slice with only JPA-related
// beans (repositories, entity manager) wired up, not the full app - faster than
// @SpringBootTest and scoped to what these tests actually need. Each test runs in its
// own transaction that is rolled back afterward, so fixtures created in setUp() never
// leak between tests.
@DataJpaTest
// Loads the "dev" profile's datasource config for this test slice.
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

    // Runs before every test to build a consistent baseline: a category, two
    // vehicles, a customer, and one existing PENDING reservation (Sep 1-5) for
    // `vehicle`. Individual tests then query against this known fixture. Names/plates are
    // suffixed with System.nanoTime() to avoid unique-constraint collisions across runs.
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

    // Verifies the overlap query correctly flags a requested range (Sep 3-7) that
    // partially overlaps the existing reservation (Sep 1-5) - this is the core check that
    // prevents double-booking a vehicle.
    @Test
    void findOverlapping_findsReservationWithOverlappingDateRange() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).hasSize(1);
    }

    // Verifies dates that don't overlap the existing reservation are not
    // flagged - guards against a too-broad query that would block legitimate,
    // non-conflicting bookings.
    @Test
    void findOverlapping_ignoresNonOverlappingDateRange() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 12),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).isEmpty();
    }

    // Verifies the overlap check is scoped to a single vehicle - the same dates
    // used for a *different* vehicle must not be reported as a conflict.
    @Test
    void findOverlapping_ignoresDifferentVehicle() {
        var result = reservationRepository.findOverlapping(otherVehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).isEmpty();
    }

    // Verifies only "blocking" statuses (e.g. PENDING/CONFIRMED/CHECKED_OUT) count
    // as an overlap - a CANCELLED reservation still exists in the DB but must not
    // prevent the same dates from being booked again.
    @Test
    void findOverlapping_ignoresStatusesNotInBlockingList() {
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 7),
                List.of(ReservationStatus.CANCELLED));

        assertThat(result).isEmpty();
    }

    // Verifies the edge case where a requested range ends exactly on the start
    // date of an existing reservation still counts as overlapping (i.e. the query's date
    // comparison is inclusive) - an off-by-one here could let two bookings share a day.
    @Test
    void findOverlapping_matchesExactBoundaryDates() {
        // requested range ends exactly on the existing reservation's start date
        var result = reservationRepository.findOverlapping(vehicle.getId(),
                LocalDate.of(2026, 8, 28), LocalDate.of(2026, 9, 1),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(result).hasSize(1);
    }

    // Verifies the existence-check query (used as a fast pre-check before the
    // full overlap query) correctly reports true when a blocking reservation exists for
    // the vehicle.
    @Test
    void existsByVehicleIdAndStatusIn_trueWhenBlockingReservationExists() {
        boolean exists = reservationRepository.existsByVehicleIdAndStatusIn(vehicle.getId(),
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT));

        assertThat(exists).isTrue();
    }

    // Verifies a customer's reservation list query is properly filtered by
    // customer ID - without this, one customer could see another customer's bookings.
    @Test
    void findByCustomerIdOrderByStartDateDesc_returnsOnlyThatCustomersReservations() {
        var result = reservationRepository.findByCustomerIdOrderByStartDateDesc(customer.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomer().getId()).isEqualTo(customer.getId());
    }
}
