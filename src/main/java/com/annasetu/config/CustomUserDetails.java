package com.annasetu.config;

import com.annasetu.entity.User;
import com.annasetu.entity.enums.ApprovalStatus;
import com.annasetu.entity.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Responsibility:
 * Custom Spring Security {@link UserDetails} adapter encapsulating the authenticated principal.
 *
 * Domain & Architecture Context:
 * - Adapts our domain {@link User} entity to Spring Security's contract.
 * - Stores runtime contextual metadata: display name, profile ID, and organization approval status.
 *
 * Interview Talking Points:
 * 1. Adapter Pattern: Decouples Spring Security interfaces from core domain entities while exposing
 *    necessary authorization authorities (prefixed with {@code ROLE_}).
 * 2. Principal Enrichment: Retaining {@code displayName} and {@code approvalStatus} inside the principal
 *    enables instantaneous header greeting rendering and authorization checks without extra database queries.
 */
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;
    private final String displayName;
    private final Long profileId;
    private final ApprovalStatus approvalStatus;

    public CustomUserDetails(User user, String displayName, Long profileId, ApprovalStatus approvalStatus) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.enabled = user.isEnabled();
        this.displayName = displayName;
        this.profileId = profileId;
        this.approvalStatus = approvalStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Long getProfileId() {
        return profileId;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public boolean isApproved() {
        return role != Role.ORGANIZATION || approvalStatus == ApprovalStatus.APPROVED;
    }
}
