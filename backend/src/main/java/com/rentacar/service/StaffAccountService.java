package com.rentacar.service;

import com.rentacar.dto.staff.StaffAccountRequest;
import com.rentacar.dto.staff.StaffAccountUpdateRequest;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(StaffAccountRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A staff account with this email already exists");
        }

        User staff = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.STAFF)
                .active(true)
                .build();

        return userRepository.save(staff);
    }

    public List<User> list() {
        return userRepository.findByRole(Role.STAFF);
    }

    public User getById(Long id) {
        return findStaff(id);
    }

    @Transactional
    public User update(Long id, StaffAccountUpdateRequest request) {
        User staff = findStaff(id);
        staff.setFirstName(request.firstName());
        staff.setLastName(request.lastName());
        return userRepository.save(staff);
    }

    @Transactional
    public User deactivate(Long id) {
        User staff = findStaff(id);
        staff.setActive(false);
        return userRepository.save(staff);
    }

    private User findStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found"));
        if (user.getRole() != Role.STAFF) {
            throw new ResourceNotFoundException("Staff account not found");
        }
        return user;
    }
}
