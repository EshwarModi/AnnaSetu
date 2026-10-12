package com.annasetu.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Responsibility:
 * Central presentation-tier exception interceptor (@ControllerAdvice) translating domain and business
 * rule exceptions into user-friendly flash messages or JSON error payloads.
 *
 * Domain & Architecture Context:
 * - Catches concurrency and domain exceptions (SlotFullException, DuplicateBookingException, CancellationCutoffException).
 * - Distinguishes browser form submissions (redirects with flash messages) from REST API fetch requests (returns JSON errors).
 * - Prevents raw stack traces or internal server error screens from surfacing to users.
 *
 * Interview Talking Points:
 * 1. Global Resilience Pattern: Centralizes exception-to-UI translation, keeping controllers clean and uniform.
 * 2. Protocol Awareness: Detects AJAX / JSON headers to respond with appropriate HTTP status codes (409 Conflict, 400 Bad Request)
 *    versus Post/Redirect/Get flow redirects.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String uri = request.getRequestURI();
        return (accept != null && accept.contains("application/json")) || (uri != null && uri.startsWith("/api/"));
    }

    @ExceptionHandler(SlotFullException.class)
    public Object handleSlotFullException(SlotFullException ex,
                                          HttpServletRequest request,
                                          RedirectAttributes redirectAttributes) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage(), "type", "SLOT_FULL"));
        }

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/donor/dashboard";
    }

    @ExceptionHandler(DuplicateBookingException.class)
    public Object handleDuplicateBookingException(DuplicateBookingException ex,
                                                 HttpServletRequest request,
                                                 RedirectAttributes redirectAttributes) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage(), "type", "DUPLICATE_BOOKING"));
        }

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/donor/dashboard";
    }

    @ExceptionHandler(CancellationCutoffException.class)
    public Object handleCancellationCutoffException(CancellationCutoffException ex,
                                                   HttpServletRequest request,
                                                   RedirectAttributes redirectAttributes) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage(), "type", "CANCELLATION_CUTOFF"));
        }

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/donor/dashboard";
    }

    @ExceptionHandler(BusinessRuleException.class)
    public Object handleBusinessRuleException(BusinessRuleException ex,
                                             HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", ex.getMessage(), "type", "BUSINESS_RULE_VIOLATION"));
        }

        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFoundException(ResourceNotFoundException ex,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  Model model) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", ex.getMessage(), "type", "NOT_FOUND"));
        }

        model.addAttribute("statusCode", 404);
        model.addAttribute("errorTitle", "404 - Page Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex,
                                         HttpServletRequest request,
                                         HttpServletResponse response,
                                         Model model) throws Exception {
        if (AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class) != null) {
            throw ex;
        }

        log.error("Unhandled exception processing request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        if (isJsonRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected internal server error occurred.", "type", "INTERNAL_SERVER_ERROR"));
        }

        model.addAttribute("statusCode", 500);
        model.addAttribute("errorTitle", "500 - Internal Server Error");
        model.addAttribute("errorMessage", "An unexpected error occurred while processing your request. Please try again later.");
        return "error/500";
    }
}
