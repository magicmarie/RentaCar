package com.rentacar.dto.customer;

import com.rentacar.entity.User;

public record CustomerProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String username,
        String driverLicenseNumber
) {
    public static CustomerProfileResponse from(User user) {
        return new CustomerProfileResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getUsername(), user.getDriverLicenseNumber());
    }
}
