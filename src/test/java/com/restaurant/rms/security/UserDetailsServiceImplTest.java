package com.restaurant.rms.security;

import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.Role;
import com.restaurant.rms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserDetailsServiceImpl}.
 *
 * <p>Uses Mockito to stub the repository — no database required.
 * Tests verify the username-OR-email fallback logic and that the returned
 * principal is a {@link UserPrincipal} with the correct authorities.</p>
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl service;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .username("chef1")
                .email("chef1@restaurant.com")
                .password("$2a$12$hashedpassword")
                .role(Role.CHEF)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername — found by username returns UserPrincipal")
    void loadByUsername_found() {
        when(userRepository.findByUsername("chef1")).thenReturn(Optional.of(activeUser));

        UserDetails result = service.loadUserByUsername("chef1");

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("chef1");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_CHEF");
        // No email lookup when username matches.
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("loadUserByUsername — username miss falls back to email")
    void loadByEmail_fallback() {
        when(userRepository.findByUsername("chef1@restaurant.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("chef1@restaurant.com")).thenReturn(Optional.of(activeUser));

        UserDetails result = service.loadUserByUsername("chef1@restaurant.com");

        assertThat(result.getUsername()).isEqualTo("chef1");
        verify(userRepository).findByUsername("chef1@restaurant.com");
        verify(userRepository).findByEmail("chef1@restaurant.com");
    }

    @Test
    @DisplayName("loadUserByUsername — no match throws UsernameNotFoundException")
    void noMatch_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Inactive user — principal has isEnabled() = false")
    void inactiveUser_principalDisabled() {
        User inactive = User.builder()
                .username("oldstaff")
                .email("old@restaurant.com")
                .password("$2a$12$hash")
                .role(Role.WAITER)
                .isActive(false)   // deactivated account
                .build();
        when(userRepository.findByUsername("oldstaff")).thenReturn(Optional.of(inactive));

        UserPrincipal principal = (UserPrincipal) service.loadUserByUsername("oldstaff");

        assertThat(principal.isEnabled()).isFalse();
        assertThat(principal.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("UserPrincipal.fromUser — maps role correctly to ROLE_ authority")
    void fromUser_mapsAuthority() {
        UserPrincipal principal = UserPrincipal.fromUser(activeUser);

        assertThat(principal.getRole()).isEqualTo(Role.CHEF);
        assertThat(principal.ge