package com.example.Expense_Tracker_App.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Expense_Tracker_App.dto.AdminUserItem;
import com.example.Expense_Tracker_App.dto.AdminUserListResponse;
import com.example.Expense_Tracker_App.dto.AdminUserUpdateRequest;
import com.example.Expense_Tracker_App.dto.AdminUserUpdateResponse;
import com.example.Expense_Tracker_App.entity.User;
import com.example.Expense_Tracker_App.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserListResponse> list(
            @RequestParam(value = "query", required = false) String query
    ) {
        try {
            String q = query == null ? "" : query.trim();
            List<User> users;
            if (q.isBlank()) {
                users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
            } else {
                users = userRepository.findByUsernameContainingIgnoreCase(q, Sort.by(Sort.Direction.DESC, "id"));
            }

            List<AdminUserItem> items = new ArrayList<>();
            for (User u : users) {
                items.add(toItem(u));
            }
            return ResponseEntity.ok(AdminUserListResponse.ok(items));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AdminUserListResponse.fail(e.getMessage()));
        }
    }

    @PostMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminUserUpdateResponse> update(
            @PathVariable("id") Long id,
            @RequestBody AdminUserUpdateRequest req,
            HttpSession session
    ) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(AdminUserUpdateResponse.fail("id가 필요합니다."));
            }

            Long currentUserId = null;
            if (session != null && session.getAttribute("USER_ID") != null) {
                Object v = session.getAttribute("USER_ID");
                currentUserId = (v instanceof Long) ? (Long) v : Long.valueOf(String.valueOf(v));
            }

            User u = userRepository.findById(id).orElse(null);
            if (u == null) {
                return ResponseEntity.badRequest().body(AdminUserUpdateResponse.fail("사용자를 찾을 수 없습니다."));
            }

            if (currentUserId != null && currentUserId.equals(u.getId())) {
                if (req != null && req.getEnabled() != null && !req.getEnabled()) {
                    return ResponseEntity.badRequest().body(AdminUserUpdateResponse.fail("본인 계정은 비활성화할 수 없습니다."));
                }
            }

            if (req != null) {
                if (req.getRole() != null) {
                    String role = String.valueOf(req.getRole()).trim().toUpperCase();
                    if (!role.isBlank()) {
                        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                            return ResponseEntity.badRequest().body(AdminUserUpdateResponse.fail("role은 USER 또는 ADMIN만 가능합니다."));
                        }
                        u.setRole(role);
                    }
                }
                if (req.getEnabled() != null) {
                    u.setEnabled(req.getEnabled());
                }
            }

            User saved = userRepository.save(u);
            if (session != null && currentUserId != null && currentUserId.equals(saved.getId())) {
                session.setAttribute("ROLE", saved.getRole());
            }

            return ResponseEntity.ok(AdminUserUpdateResponse.ok(toItem(saved)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AdminUserUpdateResponse.fail(e.getMessage()));
        }
    }

    private AdminUserItem toItem(User u) {
        if (u == null) return null;
        return new AdminUserItem(
                u.getId(),
                u.getUsername(),
                u.getRole(),
                u.getEnabled(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}
