package com.annasetu.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Responsibility:
 * Web MVC integration test for {@link HomeController}.
 *
 * Domain & Architecture Context:
 * - Ensures public visitors can access the platform landing page without authentication barriers.
 * - Confirms proper Thymeleaf model population (stats, organizations) and template resolution.
 *
 * Interview Talking Points:
 * 1. MockMvc allows fast, deterministic HTTP testing of controllers and Thymeleaf rendering
 *    without spinning up a full HTTP server port.
 * 2. Asserts view names and key DOM strings to verify SSR markup integrity.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / returns 200 OK and renders AnnaSetu landing page with live statistics")
    void testHomePageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("organizations"))
                .andExpect(model().attributeExists("cities"))
                .andExpect(model().attributeExists("mealSlots"))
                .andExpect(content().string(containsString("AnnaSetu")))
                .andExpect(content().string(containsString("How AnnaSetu Works")))
                .andExpect(content().string(containsString("Search Slots")))
                .andExpect(content().string(containsString("Asha Kiran Children Home")));
    }
}
