package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    User registerUser(UserRegistrationDto registrationDto);

    boolean confirmEmail(String token);

    User getCurrentUser();
    
    @Deprecated
    List<User> getAllUsers();

    Page<User> getAllUsers(Pageable pageable);

    Long getCurrentUserId();

    void updateUserRole(Long userId, Role newRole);

    void updateUserStatus(Long userId, UserStatus newStatus);
}