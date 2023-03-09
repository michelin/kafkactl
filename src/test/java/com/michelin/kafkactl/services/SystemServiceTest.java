package com.michelin.kafkactl.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldGetAllEnv() {
        assertEquals(SystemService.getEnv().size(), System.getenv().size());
    }
}
