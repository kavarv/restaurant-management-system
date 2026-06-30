package com.restaurant.rms.security;

import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Enriched UserDetails principal that exposes application-specific fields
 * (id, email, role) beyond what Spring Security's built-in User class offers.
 *
 * <p>Using a custom principal means controllers can do:
 * <pre>
 *   {@literal @}AuthenticationPrincipal UserPrincipal me = ...
 *   model.addAttribute("userId", me.getId());
 * </pre>
 * without hitting the database again.</p>
 */
@Getter
public class UserPrincipal implements UserDetails {

    // ── Application fields exposed to controllers ──────────────────────────────
    private final Long   id;
    private final String username;
    private final String email;
    /** Derived from username — User entity has no separate fullName column. */
    private final String fullName;
    private final String password;
    private final Role   role;

    // ── Spring Security contract fields ────────────────────────────────────────
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    /** Private — callers must use the factory method. */
    private UserPrincipal(User user) {
        this.id                  = user.getId();
        this.username            = user.getUsername();
        this.email               = user.getEmail();
        // User entity has no fullName column; use username as display name.
        this.fullName            = user.getUsername();
        this.password            = user.getPassword();
        this.role                = user.getRole();

        // "ROLE_" prefix is required by Spring Security's hasRole() checks.
        this.authorities         = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        this.accountNonExpired   = true;
        // Lock the account if isActive == false.
        this.accountNonLocked    = Boolean.TRUE.equals(user.getIsActive());
        this.credentialsNonExpired = true;
        // Disabled means Spring Security rejects authentication immediately.
        this.enabled             = Boolean.TRUE.equals(user.getIsActive());
    }

    /**
     * Static factory — converts a JPA {@link User} entity into a Spring Security
     * principal. Called exclusively from {@link UserDetailsServiceImpl}.
     */
    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(user);
    }

    // ── UserDetails contract ───────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentia