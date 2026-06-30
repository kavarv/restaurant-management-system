package com.restaurant.rms.util.mapper;

import com.restaurant.rms.dto.response.UserResponse;
import com.restaurant.rms.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for {@link User} ↔ DTO conversions.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.from(us