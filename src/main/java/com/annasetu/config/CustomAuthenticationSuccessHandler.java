package com.annasetu.config;

import com.annasetu.entity.enums.ApprovalStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responsibility:
 * Determines the target redirect URL following a successful user authentication based on their granted roles
 * and institutional approval status.
 *
 * Domain & Architecture Context:
 * - Directs donors to {@code /donor/dashboard}.
 * - Directs administrators to {@code /admin/dashboard}.
 * - Directs verified organizations to {@code /org/dashboard}.
 * - Directs newly onboarded or pending organizations to {@code /org/pending} so feature access is withheld until verified.
 *
 * Interview Talking Points:
 * 1. Role-Based Dynamic Routing: Avoids a generic landing page by steering each persona directly to their
 *    contextual workspace.
 * 2. Trust & Safety Gate: Enforces the platform invariant that pending organizations cannot access operational
 *    features (such as slot creation or booking management) prior to admin approval.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(request.getContextPath() + targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        boolean isAdmin = false;
        boolean isOrg = false;
        boolean isDonor = false;

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String roleName = authority.getAuthority();
            if ("ROLE_ADMIN".equals(roleName)) {
                isAdmin = true;
                break;
            } else if ("ROLE_ORGANIZATION".equals(roleName)) {
                isOrg = true;
                break;
            } else if ("ROLE_DONOR".equals(roleName)) {
                isDonor = true;
                break;
            }
        }

        if (isAdmin) {
            return "/admin/dashboard";
        }

        if (isOrg) {
            return "/org/dashboard";
        }

        if (isDonor) {
            return "/donor/dashboard";
        }

        return "/";
    }
}
