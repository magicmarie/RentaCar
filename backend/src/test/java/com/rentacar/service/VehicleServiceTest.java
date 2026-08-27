package com.rentacar.service;

import com.rentacar.dto.vehicle.VehicleRequest;
import com.rentacar.dto.vehicle.VehicleUpdateRequest;
import com.rentacar.entity.Category;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.CategoryRepository;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Category economy;

    @BeforeEach
    void setUp() {
        economy = Category.builder().id(1L).name("Economy").build();
    }

    @Test
    void create_rejectsDuplicateLicensePlate() {
        var request = new VehicleRequest("Toyota", "Corolla", 2022, "ECO-101", 5, 1L);
        when(vehicleRepository.existsByLicensePlateIgnoreCase("ECO-101")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownCategory() {
        var request = new VehicleRequest("Toyota", "Corolla", 2022, "ECO-101", 5, 99L);
        when(vehicleRepository.existsByLicensePlateIgnoreCase("ECO-101")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesWithAvailableStatus() {
        var request = new VehicleRequest("Toyota", "Corolla", 2022, "ECO-101", 5, 1L);
        when(vehicleRepository.existsByLicensePlateIgnoreCase("ECO-101")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(economy));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.create(request);

        assertThat(result.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(result.getLicensePlate()).isEqualTo("ECO-101");
    }

    @Test
    void update_leavesLicensePlateUntouched() {
        Vehicle existing = Vehicle.builder().id(10L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build();
        var request = new VehicleUpdateRequest("Honda", "Civic", 2023, 4, 1L);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(economy));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        Vehicle result = vehicleService.update(10L, request);

        assertThat(result.getLicensePlate()).isEqualTo("ECO-101");
        assertThat(result.getMake()).isEqualTo("Honda");
        assertThat(result.getSeatingCapacity()).isEqualTo(4);
    }

    @Test
    void delete_rejectsWhenVehicleHasActiveReservation() {
        Vehicle vehicle = Vehicle.builder().id(10L).category(economy).status(VehicleStatus.AVAILABLE).build();
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.existsByVehicleIdAndStatusIn(eq(10L), anyList())).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.delete(10L))
                .isInstanceOf(InvalidStateException.class);

        verify(vehicleRepository, never()).delete(any());
    }

    @Test
    void delete_succeedsWhenNoActiveReservations() {
        Vehicle vehicle = Vehicle.builder().id(10L).category(economy).status(VehicleStatus.AVAILABLE).build();
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(reservationRepository.existsByVehicleIdAndStatusIn(eq(10L), anyList())).thenReturn(false);

        vehicleService.delete(10L);

        verify(vehicleRepository).delete(vehicle);
    }

    @Test
    void getById_throwsWhenMissing() {
        when(vehicleRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
