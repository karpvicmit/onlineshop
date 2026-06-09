package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;

import java.util.List;

public interface UserService {

    User registerUser(UserRegistrationDto registrationDto);

    User getCurrentUser();

    List<User> getAllUsers();

    void updateUserRole(Long userId, Role newRole);

    void updateUserStatus(Long userId, UserStatus newStatus);
}