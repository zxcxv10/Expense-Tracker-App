package com.example.Expense_Tracker_App.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Expense_Tracker_App.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> findByUsernameContainingIgnoreCase(String username, Sort sort);
}
