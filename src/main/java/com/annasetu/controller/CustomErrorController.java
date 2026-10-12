package com.annasetu.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Responsibility:
 * Custom application-wide error controller providing tailored, user-friendly 403 (Forbidden),
 * 404 (Not Found), and generic error views styled consistently with the platform layout.
 *
 * Domain & Architecture Context:
 * - Implements Spring Boot's {@link ErrorController} interface to intercept unhandled HTTP exceptions.
 * - Explicitly maps {@code /403} and {@code /404} direct navigation routes for security and error dispatchers.
 *
 * Interview Talking Points:
 * 1. Professional Error Handling: Replaces Spring Boot's default "Whitelabel Error Page" with
 *    semantic, accessible HTML views.
 * 2. Security Defense: Masks internal exception stack traces from production responses to prevent
 *    information disclosure vulnerabilities.
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {
            }
        }

        model.addAttribute("statusCode", statusCode);

        if (statusCode == HttpStatus.FORBIDDEN.value()) {
            model.addAttribute("errorTitle", "403 - Access Forbidden");
            model.addAttribute("errorMessage", "You do not have administrative or appropriate permissions to access this area.");
            return "error/403";
        } else if (statusCode == HttpStatus.NOT_FOUND.value()) {
            model.addAttribute("errorTitle", "404 - Page Not Found");
            model.addAttribute("errorMessage", "The page or resource you are looking for does not exist or has been moved.");
            return "error/404";
        } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            model.addAttribute("errorTitle", "500 - Internal Server Error");
            model.addAttribute("errorMessage", "We encountered an issue processing your request. Please try again later.");
            return "error/500";
        }

        model.addAttribute("errorTitle", "An Unexpected Error Occurred");
        model.addAttribute("errorMessage", "We encountered an issue processing your request. Please try again later.");
        return "error/error";
    }

    @GetMapping("/403")
    public String accessDenied(HttpServletRequest request, Model model) {
        model.addAttribute("statusCode", 403);
        model.addAttribute("errorTitle", "403 - Access Forbidden");
        Object msg = request.getAttribute("errorMessage");
        model.addAttribute("errorMessage", msg != null ? msg : "You do not have permission to access this resource.");
        return "error/403";
    }

    @GetMapping("/404")
    public String notFound(Model model) {
        model.addAttribute("statusCode", 404);
        model.addAttribute("errorTitle", "404 - Page Not Found");
        model.addAttribute("errorMessage", "The requested page could not be located.");
        return "error/404";
    }
}
