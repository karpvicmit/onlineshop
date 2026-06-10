package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.dto.user.ProfileUpdateDto;

public interface ProfileService {
    void updateProfile(User user, ProfileUpdateDto profileDto);
}