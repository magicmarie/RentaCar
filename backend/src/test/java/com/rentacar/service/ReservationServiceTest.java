package com.rentacar.service;

import com.rentacar.entity.*;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.UserRepository;
import com.rentacar.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BillingService billingService;

    @InjectMocks
    private ReservationService reservationService;

    private User customer;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        vehicle = Vehicle.builder().id(10L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build();
        customer = User.builder().id(100L).firstName("Jane").lastName("Doe").email("jane@example.com")
                .role(Role.CUSTOMER).active(true).build();
    }

    @Test
    void createReservation_rejectsOverlappingBooking() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.findOverlapping(eq(10L), eq(start), eq(end), anyList()))
                .thenReturn(List.of(Reservation.builder().id(1L).status(ReservationStatus.PENDING).build()));

        assertThatThrownBy(() -> reservationService.createReservation(100L, 10L, start, end))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("no longer available");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_succeedsWhenNoOverlap() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        when(userRepository.findById(100L)).thenReturn(Optional.of(customer));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.findOverlapping(eq(10L), eq(start), eq(end), anyList()))
                .thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(100L, 10L, start, end);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.getVehicle()).isEqualTo(vehicle);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_rejectsWhenAlreadyCheckedOut() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.CHECKED_OUT).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(5L, 100L, false))
                .isInstanceOf(InvalidStateException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_succeedsWhenPending() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.cancelReservation(5L, 100L, false);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void createReservation_rejectsEndDateNotAfterStartDate() {
        LocalDate sameDay = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> reservationService.createReservation(100L, 10L, sameDay, sameDay))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("End date");

        verifyNoInteractions(reservationRepository, vehicleRepository, userRepository);
    }

    @Test
    void cancelReservation_rejectsWhenNotOwnerAndNotStaffOrAdmin() {
        User otherCustomer = User.builder().id(200L).role(Role.CUSTOMER).build();
        Reservation reservation = Reservation.builder()
                .id(5L).customer(otherCustomer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(5L, 100L, false))
                .isInstanceOf(AccessDeniedException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void cancelReservation_staffCanCancelOnBehalfOfCustomer() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.cancelReservation(5L, 999L, true);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void getById_rejectsWhenNotOwnerAndNotStaffOrAdmin() {
        User otherCustomer = User.builder().id(200L).role(Role.CUSTOMER).build();
        Reservation reservation = Reservation.builder()
                .id(5L).customer(otherCustomer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getById(5L, 100L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkOut_rejectsWhenReservationNotPendingOrConfirmed() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.COMPLETED).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.checkOut(5L, LocalDateTime.now()))
                .isInstanceOf(InvalidStateException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void checkOut_rejectsWhenVehicleAlreadyRented() {
        vehicle.setStatus(VehicleStatus.RENTED);
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.checkOut(5L, LocalDateTime.now()))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void checkOut_transitionsReservationAndVehicleOnSuccess() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        LocalDateTime pickup = LocalDateTime.now();

        Reservation result = reservationService.checkOut(5L, pickup);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        assertThat(result.getPickupDateTime()).isEqualTo(pickup);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RENTED);
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void processReturn_rejectsWhenReservationNotCheckedOut() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.PENDING).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.processReturn(5L, LocalDate.now(), "fine", false))
                .isInstanceOf(InvalidStateException.class);

        verifyNoInteractions(billingService);
    }

    @Test
    void processReturn_setsVehicleUnderMaintenanceWhenFlagged() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.CHECKED_OUT)
                .pickupDateTime(LocalDateTime.now().minusDays(2)).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(billingService.generateBill(reservation)).thenReturn(Bill.builder().id(1L).build());

        reservationService.processReturn(5L, LocalDate.now(), "scratched bumper", true);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.UNDER_MAINTENANCE);
    }

    @Test
    void processReturn_completesReservationAndDelegatesToBillingService() {
        Reservation reservation = Reservation.builder()
                .id(5L).customer(customer).vehicle(vehicle).status(ReservationStatus.CHECKED_OUT)
                .pickupDateTime(LocalDateTime.now().minusDays(2)).build();
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        Bill expectedBill = Bill.builder().id(1L).build();
        when(billingService.generateBill(reservation)).thenReturn(expectedBill);

        Bill result = reservationService.processReturn(5L, LocalDate.now(), "all good", false);

        assertThat(result).isEqualTo(expectedBill);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(captor.capture());
        assertThat(captor.getValue().getConditionNotes()).isEqualTo("all good");
    }

    @Test
    void search_blankQueryIsTreatedAsNoFilter() {
        when(reservationRepository.search(ReservationStatus.PENDING, null)).thenReturn(List.of());

        reservationService.search(ReservationStatus.PENDING, "   ");

        verify(reservationRepository).search(ReservationStatus.PENDING, null);
    }

    @Test
    void search_lowercasesAndWrapsQueryForLikeMatching() {
        when(reservationRepository.search(null, "%doe%")).thenReturn(List.of());

        reservationService.search(null, "  Doe  ");

        verify(reservationRepository).search(null, "%doe%");
    }

    @Test
    void searchAvailability_excludesVehiclesNotAvailableOrWithOverlap() {
        Category economy = vehicle.getCategory();
        Vehicle underMaintenance = Vehicle.builder().id(11L).category(economy).status(VehicleStatus.UNDER_MAINTENANCE).build();
        Vehicle overlapping = Vehicle.builder().id(12L).category(economy).status(VehicleStatus.AVAILABLE).build();
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle, underMaintenance, overlapping));
        when(reservationRepository.findOverlapping(eq(10L), eq(start), eq(end), anyList())).thenReturn(List.of());
        when(reservationRepository.findOverlapping(eq(12L), eq(start), eq(end), anyList()))
                .thenReturn(List.of(Reservation.builder().id(1L).status(ReservationStatus.PENDING).build()));

        var result = reservationService.searchAvailability(start, end, null);

        assertThat(result).containsExactly(vehicle);
    }
}
