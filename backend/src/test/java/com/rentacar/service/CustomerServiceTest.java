package com.rentacar.service;

import com.rentacar.dto.customer.CustomerProfileUpdateRequest;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    // @Mock stubs the user repository so no real database is needed.
    @Mock
    private UserRepository userRepository;

    // @InjectMocks builds a real CustomerService wired with the mock above.
    @InjectMocks
    private CustomerService customerService;

    // Verifies a staff account can't be fetched through the customer-profile
    // endpoint, even though it exists as a User row (id lookups shouldn't
    // leak across roles).
    @Test
    void getProfile_throwsWhenUserIsNotCustomer() {
        User staff = User.builder().id(2L).role(Role.STAFF).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> customerService.getProfile(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies a nonexistent user id fails clearly rather than returning null.
    @Test
    void getProfile_throwsWhenMissing() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getProfile(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies the happy path: an existing customer id returns that customer.
    @Test
    void getProfile_returnsCustomer() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).firstName("Jane").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));

        User result = customerService.getProfile(3L);

        assertThat(result.getFirstName()).isEqualTo("Jane");
    }

    // Verifies profile updates are limited to name fields: email and driver
    // license, which are tied to identity/uniqueness checks, must not be
    // silently overwritten by this endpoint.
    @Test
    void updateProfile_changesNameButNotEmailOrLicense() {
        User customer = User.builder().id(3L).role(Role.CUSTOMER).firstName("Old").lastName("Name")
                .email("jane@example.com").driverLicenseNumber("DL-1").build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = customerService.updateProfile(3L, new CustomerProfileUpdateRequest("New", "Name"));

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
        assertThat(result.getDriverLicenseNumber()).isEqualTo("DL-1");
    }
}
