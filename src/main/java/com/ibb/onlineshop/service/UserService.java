package com.ibb.onlineshop.service;

import com.ibb.onlineshop.dto.user.UserRegistrationDto;
import com.ibb.onlineshop.entity.User;

public interface UserService {
    User registerUser(UserRegistrationDto registrationDto);
    User getCurrentUser();
}