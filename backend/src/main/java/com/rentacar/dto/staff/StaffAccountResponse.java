package com.rentacar.dto.staff;

import com.rentacar.entity.User;

public record StaffAccountResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean active
) {
    public static StaffAccountResponse from(User user) {
        return new StaffAccountResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.isActive());
    }
}
