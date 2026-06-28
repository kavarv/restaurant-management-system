package com.restaurant.rms.security;

import com.restaurant.rms.entity.User;
import com.restaurant.rms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @deprecated Superseded by {@link UserDetailsServiceImpl}, which also supports
 *             login by email and returns a richer {@link UserPrincipal}.
 *             The {@code @Service} annotation has been intentionally removed so
 *             Spring registers only one {@link UserDetailsService} bean
 *             ({@link UserDetailsServiceImpl}).  Keep this class during the
 *             migration period to avoid breaking any direct injection references.
 */
@Deprecated(since = "2.0", forRemoval = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(!user.getIsActive())
                .credentialsExpired(false)
                .disabled(!user.getIsActive())
                .build();
    }
}
