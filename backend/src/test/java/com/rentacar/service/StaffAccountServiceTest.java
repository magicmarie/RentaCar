package com.rentacar.service;

import com.rentacar.dto.staff.StaffAccountRequest;
import com.rentacar.dto.staff.StaffAccountUpdateRequest;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTest {

    // @Mock stubs out persistence and password hashing so account logic is
    // tested in isolation from the database and real crypto.
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    // @InjectMocks builds a real StaffAccountService wired with the mocks above.
    @InjectMocks
    private StaffAccountService staffAccountService;

    // Verifies a staff account can't be created with an email already in use.
    @Test
    void create_rejectsDuplicateEmail() {
        var request = new StaffAccountRequest("Front", "Desk", "staff@rentacar.com", "password123");
        when(userRepository.existsByEmail("staff@rentacar.com")).thenReturn(true);

        assertThatThrownBy(() -> staffAccountService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    // Verifies new staff accounts are created active, with the STAFF role,
    // and store only the hashed password (never the plaintext).
    @Test
    void create_savesActiveStaffAccountWithEncodedPassword() {
        var request = new StaffAccountRequest("Front", "Desk", "staff@rentacar.com", "password123");
        when(userRepository.existsByEmail("staff@rentacar.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = staffAccountService.create(request);

        assertThat(result.getRole()).isEqualTo(Role.STAFF);
        assertThat(result.isActive()).isTrue();
        assertThat(result.getPasswordHash()).isEqualTo("hashed");
    }

    // Verifies a customer's id can't be looked up through the staff-account
    // endpoint, keeping the two account types from leaking into each other.
    @Test
    void getById_throwsWhenUserIsNotStaff() {
        User customer = User.builder().id(5L).role(Role.CUSTOMER).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> staffAccountService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Verifies updating a staff account only changes the name fields,
    // leaving email (used for login) untouched.
    @Test
    void update_changesNameOnly() {
        User staff = User.builder().id(5L).role(Role.STAFF).firstName("Old").lastName("Name")
                .email("staff@rentacar.com").active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = staffAccountService.update(5L, new StaffAccountUpdateRequest("New", "Name"));

        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getEmail()).isEqualTo("staff@rentacar.com");
    }

    // Verifies deactivating a staff account flips it inactive rather than
    // deleting it, e.g. so the account can be re-enabled later and its
    // history preserved.
    @Test
    void deactivate_setsActiveFalse() {
        User staff = User.builder().id(5L).role(Role.STAFF).active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = staffAccountService.deactivate(5L);

        assertThat(result.isActive()).isFalse();
    }
}
