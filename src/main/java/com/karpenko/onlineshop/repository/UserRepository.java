package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailConfirmationToken(String token);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);
}