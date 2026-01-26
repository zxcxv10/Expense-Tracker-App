package com.example.Expense_Tracker_App.controller;

import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.AuthLoginRequest;
import com.example.Expense_Tracker_App.dto.AuthResponse;
import com.example.Expense_Tracker_App.dto.AuthSignupRequest;
import com.example.Expense_Tracker_App.entity.User;
import com.example.Expense_Tracker_App.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> me(HttpSession session) {
        try {
            if (session == null) {
                return ResponseEntity.ok(AuthResponse.fail("NOT_LOGGED_IN"));
            }

            Object userIdObj = session.getAttribute("USER_ID");
            Object usernameObj = session.getAttribute("USERNAME");
            Object roleObj = session.getAttribute("ROLE");

            if (userIdObj == null || usernameObj == null) {
                return ResponseEntity.ok(AuthResponse.fail("NOT_LOGGED_IN"));
            }

            Long userId = (userIdObj instanceof Long) ? (Long) userIdObj : Long.valueOf(String.valueOf(userIdObj));
            String username = String.valueOf(usernameObj);
            String role = roleObj == null ? null : String.valueOf(roleObj);

            return ResponseEntity.ok(AuthResponse.ok(userId, username, role));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthSignupRequest req, HttpSession session) {
        try {
            String username = req == null ? null : req.getUsername();
            String password = req == null ? null : req.getPassword();

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("username은 필수입니다."));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("password는 필수입니다."));
            }

            String u = username.trim();
            if (u.length() > 50) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("username이 너무 깁니다."));
            }

            if (userRepository.existsByUsername(u)) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("이미 존재하는 username입니다."));
            }

            User user = new User();
            user.setUsername(u);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("USER");
            User saved = userRepository.save(user);

            if (session != null) {
                session.setAttribute("USER_ID", saved.getId());
                session.setAttribute("USERNAME", saved.getUsername());
                session.setAttribute("ROLE", saved.getRole());
            }

            return ResponseEntity.ok(AuthResponse.ok(saved.getId(), saved.getUsername(), saved.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> login(@RequestBody AuthLoginRequest req, HttpSession session) {
        try {
            String username = req == null ? null : req.getUsername();
            String password = req == null ? null : req.getPassword();

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("username은 필수입니다."));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("password는 필수입니다."));
            }

            String u = username.trim();
            Optional<User> opt = userRepository.findByUsername(u);
            if (opt.isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("아이디/비밀번호가 올바르지 않습니다."));
            }

            User user = opt.get();
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.badRequest().body(AuthResponse.fail("아이디/비밀번호가 올바르지 않습니다."));
            }

            if (session != null) {
                session.setAttribute("USER_ID", user.getId());
                session.setAttribute("USERNAME", user.getUsername());
                session.setAttribute("ROLE", user.getRole());
            }

            return ResponseEntity.ok(AuthResponse.ok(user.getId(), user.getUsername(), user.getRole()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> logout(HttpSession session) {
        try {
            if (session != null) {
                session.invalidate();
            }
            return ResponseEntity.ok(AuthResponse.ok(null, null, null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.fail(e.getMessage()));
        }
    }
}
