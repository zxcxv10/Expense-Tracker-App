package com.example.Expense_Tracker_App.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.Expense_Tracker_App.entity.User;
import com.example.Expense_Tracker_App.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public AuthInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        Object userId = (session == null) ? null : session.getAttribute("USER_ID");
        Object usernameObj = (session == null) ? null : session.getAttribute("USERNAME");
        Object roleObj = (session == null) ? null : session.getAttribute("ROLE");

        String path = request == null ? "" : String.valueOf(request.getRequestURI());

        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\":false,\"error\":\"UNAUTHORIZED\"}");
            return false;
        }

        String username = usernameObj == null ? null : String.valueOf(usernameObj);
        if (username != null && !username.isBlank()) {
            try {
                User u = userRepository.findByUsername(username).orElse(null);
                if (u != null && u.getEnabled() != null && !u.getEnabled()) {
                    try {
                        session.invalidate();
                    } catch (Exception ignore) {
                    }
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":false,\"error\":\"DISABLED\"}");
                    return false;
                }
            } catch (Exception ignore) {
                // ignore
            }
        }

        if (path != null && path.startsWith("/api/admin")) {
            String role = roleObj == null ? "" : String.valueOf(roleObj);
            if (!"ADMIN".equalsIgnoreCase(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"success\":false,\"error\":\"FORBIDDEN\"}");
                return false;
            }
        }

        return true;
    }
}
