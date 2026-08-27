package com.rentacar.repository;

import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByDriverLicenseNumber(String driverLicenseNumber);

    List<User> findByRole(Role role);
}
