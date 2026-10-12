package com.annasetu.controller;

import com.annasetu.dto.AdminStatsDto;
import com.annasetu.entity.Booking;
import com.annasetu.entity.OrganizationProfile;
import com.annasetu.entity.User;
import com.annasetu.exception.BusinessRuleException;
import com.annasetu.service.AdminService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Responsibility:
 * Presentation controller managing administrator console workflows:
 * reviewing platform metrics, vetting partner applications (approval & rejection with reason),
 * tracking recent platform-wide meal sponsorships, and managing user security statuses.
 *
 * Domain & Architecture Context:
 * - Secures all {@code /admin/**} endpoints strictly under {@code ROLE_ADMIN}.
 * - Delegates all state changes and metrics generation to {@link AdminService}.
 * - Implements the Post/Redirect/Get (PRG) pattern with informative flash feedback.
 *
 * Interview Talking Points:
 * 1. Persona Workspace Cohesion: Concentrates administrative platform oversight in a dedicated controller.
 * 2. Governance Auditing: Captures required reasons when rejecting applications to promote institutional transparency.
 * 3. Self-Disabling Protection: Captures user identity via {@link Authentication} to prevent accidental administrator lockouts.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Admin Dashboard: Displays aggregated metrics, pending applications, recent platform bookings,
     * partner organization directory, and user access controls.
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminStatsDto stats = adminService.getAdminStats();
        List<Booking> recentBookings = adminService.getRecentBookings(15);
        List<OrganizationProfile> pendingOrgs = adminService.getPendingOrganizations();
        List<OrganizationProfile> allOrgs = adminService.getAllOrganizations();
        List<User> allUsers = adminService.getAllUsers();

        model.addAttribute("stats", stats);
        model.addAttribute("recentBookings", recentBookings);
        model.addAttribute("pendingOrgs", pendingOrgs);
        model.addAttribute("allOrgs", allOrgs);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("pageTitle", "Admin Console - AnnaSetu");
        return "admin/dashboard";
    }

    /**
     * Approves an organization's application, granting them ability to manage calendar slots.
     */
    @PostMapping("/organizations/{id}/approve")
    public String approveOrganization(@PathVariable("id") Long orgId,
                                      RedirectAttributes redirectAttributes) {
        OrganizationProfile org = adminService.approveOrganization(orgId);
        redirectAttributes.addFlashAttribute("successMessage",
                "Organization '" + org.getName() + "' has been approved successfully. Slot management is unlocked.");
        return "redirect:/admin/dashboard";
    }

    /**
     * Rejects an organization's application with a documented reason.
     */
    @PostMapping("/organizations/{id}/reject")
    public String rejectOrganization(@PathVariable("id") Long orgId,
                                     @RequestParam("reason") String reason,
                                     RedirectAttributes redirectAttributes) {
        OrganizationProfile org = adminService.rejectOrganization(orgId, reason);
        redirectAttributes.addFlashAttribute("successMessage",
                "Organization '" + org.getName() + "' has been rejected. Reason recorded and coordinator notified.");
        return "redirect:/admin/dashboard";
    }

    /**
     * Enables or disables a user's platform access.
     */
    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable("id") Long userId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        try {
            User updated = adminService.toggleUserStatus(userId, authentication != null ? authentication.getName() : null);
            String status = updated.isEnabled() ? "ENABLED" : "DISABLED";
            redirectAttributes.addFlashAttribute("successMessage",
                    "User account '" + updated.getEmail() + "' is now " + status + ".");
        } catch (BusinessRuleException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
