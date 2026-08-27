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

@ExtendWith(MockitoExtension.class)
class StaffAccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StaffAccountService staffAccountService;

    @Test
    void create_rejectsDuplicateEmail() {
        var request = new StaffAccountRequest("Front", "Desk", "staff@rentacar.com", "password123");
        when(userRepository.existsByEmail("staff@rentacar.com")).thenReturn(true);

        assertThatThrownBy(() -> staffAccountService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

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

    @Test
    void getById_throwsWhenUserIsNotStaff() {
        User customer = User.builder().id(5L).role(Role.CUSTOMER).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> staffAccountService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

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

    @Test
    void deactivate_setsActiveFalse() {
        User staff = User.builder().id(5L).role(Role.STAFF).active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(staff));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = staffAccountService.deactivate(5L);

        assertThat(result.isActive()).isFalse();
    }
}
