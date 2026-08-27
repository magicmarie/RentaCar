package com.rentacar.config;

import com.rentacar.entity.Category;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.repository.CategoryRepository;
import com.rentacar.repository.UserRepository;
import com.rentacar.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = User.builder()
                .firstName("System")
                .lastName("Admin")
                .email("admin@rentacar.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);

        User staff = User.builder()
                .firstName("Front")
                .lastName("Desk")
                .email("staff@rentacar.com")
                .passwordHash(passwordEncoder.encode("staff123"))
                .role(Role.STAFF)
                .active(true)
                .build();
        userRepository.save(staff);

        User customer = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("customer@rentacar.com")
                .username("janedoe")
                .passwordHash(passwordEncoder.encode("customer123"))
                .role(Role.CUSTOMER)
                .driverLicenseNumber("DL-0001")
                .active(true)
                .build();
        userRepository.save(customer);

        Category economy = categoryRepository.save(Category.builder()
                .name("Economy").dailyRate(new BigDecimal("40.00")).build());
        Category suv = categoryRepository.save(Category.builder()
                .name("SUV").dailyRate(new BigDecimal("70.00")).build());
        Category luxury = categoryRepository.save(Category.builder()
                .name("Luxury").dailyRate(new BigDecimal("120.00")).build());

        vehicleRepository.save(Vehicle.builder().make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build());
        vehicleRepository.save(Vehicle.builder().make("Honda").model("Civic").year(2023)
                .licensePlate("ECO-102").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build());
        vehicleRepository.save(Vehicle.builder().make("Toyota").model("RAV4").year(2022)
                .licensePlate("SUV-201").seatingCapacity(5).category(suv).status(VehicleStatus.AVAILABLE).build());
        vehicleRepository.save(Vehicle.builder().make("Ford").model("Explorer").year(2023)
                .licensePlate("SUV-202").seatingCapacity(7).category(suv).status(VehicleStatus.AVAILABLE).build());
        vehicleRepository.save(Vehicle.builder().make("BMW").model("5 Series").year(2023)
                .licensePlate("LUX-301").seatingCapacity(5).category(luxury).status(VehicleStatus.AVAILABLE).build());
        vehicleRepository.save(Vehicle.builder().make("Mercedes-Benz").model("GLE").year(2023)
                .licensePlate("LUX-302").seatingCapacity(5).category(luxury).status(VehicleStatus.AVAILABLE).build());

        log.info("Seeded dev data. Login with admin@rentacar.com / admin123, staff@rentacar.com / staff123, customer@rentacar.com / customer123");
    }
}
