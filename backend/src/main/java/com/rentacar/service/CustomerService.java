package com.rentacar.service;

import com.rentacar.dto.customer.CustomerProfileUpdateRequest;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserRepository userRepository;

    public User getProfile(Long customerId) {
        return findCustomer(customerId);
    }

    @Transactional
    public User updateProfile(Long customerId, CustomerProfileUpdateRequest request) {
        User customer = findCustomer(customerId);
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        return userRepository.save(customer);
    }

    private User findCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found");
        }
        return user;
    }
}
