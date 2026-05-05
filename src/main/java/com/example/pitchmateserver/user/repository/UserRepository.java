package com.example.pitchmateserver.user.repository;

import com.example.pitchmateserver.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<User> findByEmail(String email);
    List<User> findByNicknameContainingAndRole(String nickname, String role);
}
