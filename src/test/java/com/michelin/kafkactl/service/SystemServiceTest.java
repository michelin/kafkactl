package com.michelin.kafkactl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SystemServiceTest {
    @Test
    void shouldGetEnv() {
        assertEquals(System.getenv("ANY_VAR_ENV"), SystemService.getEnv("ANY_VAR_ENV"));
    }

    @Test
    void shouldGetProperty() {
        assertEquals(System.getProperty("ANY_PROP"), SystemService.getProperty("ANY_PROP"));
    }

    @Test
    void shouldSetProperty() {
        SystemService.setProperty("MY_PROP", "VALUE");
        assertEquals("VALUE", System.getProperty("MY_PROP"));
    }
}
