package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.User;

public interface UserService {
    User registerUser(UserRegistrationDto registrationDto);
    User getCurrentUser();
}