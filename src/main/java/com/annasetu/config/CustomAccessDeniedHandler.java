package com.annasetu.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responsibility:
 * Custom handler invoked whenever an authenticated principal attempts to access a protected URL
 * for which they lack the required authorities (HTTP 403 Forbidden).
 *
 * Domain & Architecture Context:
 * - Prevents default ugly server error pages by forwarding to a styled {@code /403} Thymeleaf error view.
 * - Provides diagnostic logging for unauthorized access attempts across security boundaries (e.g., Donors accessing /admin).
 *
 * Interview Talking Points:
 * 1. Granular Access Control Exception Handling: Seamlessly intercepts Spring Security's
 *    {@link AccessDeniedException} and renders a friendly user interface with navigation guidance.
 * 2. Forward vs Redirect: Uses forward to preserve the request attribute and status code (403).
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute("errorMessage", "You do not have permission to access this resource.");
        request.setAttribute("requestedUri", request.getRequestURI());
        request.getRequestDispatcher("/403").forward(request, response);
    }
}
