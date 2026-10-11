package com.bgsistemas.api.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceTest {

    @Test
    void shouldCreateServiceWithGivenAttributes() {
        Service service = new Service(1L, "Web Design", "Custom website design", "design.jpg");

        assertEquals(1L, service.getId());
        assertEquals("Web Design", service.getName());
        assertEquals("Custom website design", service.getDescription());
        assertEquals("design.jpg", service.getImageUrl());
    }
}