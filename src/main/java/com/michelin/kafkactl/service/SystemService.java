package com.michelin.kafkactl.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The system service.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemService {
    /**
     * Get a system environment variable by name.
     *
     * @param name The var name
     * @return The system environment variable
     */
    public static String getEnv(String name) {
        return System.getenv(name);
    }

    /**
     * Get a system property by name.
     *
     * @param name The property name
     * @return The system property name
     */
    public static String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * Set a system property.
     *
     * @param key   The name
     * @param value The value
     */
    public static void setProperty(String key, String value) {
        System.setProperty(key, value);
    }
}
