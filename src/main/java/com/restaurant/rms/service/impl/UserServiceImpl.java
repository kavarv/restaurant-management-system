package com.restaurant.rms.service.impl;

import com.restaurant.rms.dto.request.RegisterRequest;
import com.restaurant.rms.dto.request.UpdateUserRequest;
import com.restaurant.rms.dto.response.PagedResponse;
import com.restaurant.rms.dto.response.UserResponse;
import com.restaurant.rms.entity.User;
import com.restaurant.rms.entity.enums.AuditAction;
import com.restaurant.rms.exception.DuplicateResourceException;
import com.restaurant.rms.exception.InvalidOperationException;
import com.restaurant.rms.exception.ResourceNotFoundException;
import com.restaurant.rms.repository.UserRepository;
import com.restaurant.rms.service.AuditService;
import com.restaurant.rms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .isActive(true)
                .build();

        User saved = userRepository.save(user);
        auditService.log("User", saved.getId(), AuditAction.CREATE, null, saved.getUsername());
        log.info("Registered new user id={} username={}", saved.getId(), saved.getUsername());
        return UserResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(Pageable pageable) {
        return PagedResponse.from(userRepository.findAll(pageable), UserResponse::from);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = getUserOrThrow(id);
        String oldEmail = user.getEmail();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("User", "email", request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User saved = userRepository.save(user);
        auditService.log("User", saved.getId(), AuditAction.UPDATE, oldEmail, saved.getEmail());
        return UserResponse.from(saved);
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserOrThrow(id);
        if (!user.getIsActive()) {
            throw new InvalidOperationException("deactivateUser", "user is already inactive");
        }
        user.setIsActive(false);
        userRepository.save(user);
        auditService.log("User", id, AuditAction.UPDATE, "active", "inactive");
        log.info("Deactivated user id={}", id);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
