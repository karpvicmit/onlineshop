package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.user.ProfileUpdateDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void updateProfile(User user, ProfileUpdateDto dto) {
        log.info("Profil-Update für Benutzer: {}", user.getEmail());

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setAddress(dto.getAddress());

        userRepository.save(user);
        log.info("Profil von {} erfolgreich aktualisiert", user.getEmail());
    }
}