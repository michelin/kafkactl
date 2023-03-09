package com.michelin.kafkactl;

import org.junit.jupiter.api.Test;

import static com.michelin.kafkactl.KafkactlConfig.KAFKACTL_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkactlConfigTest {

    private final KafkactlConfig kafkactlConfig = new KafkactlConfig();

    @Test
    void shouldGetConfigDirectoryFromUserHome() {
        System.setProperty(KAFKACTL_CONFIG, "");

        String actual = kafkactlConfig.getConfigDirectory();

        assertEquals(System.getProperty("user.home") + "/.kafkactl", actual);
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfig() {
        System.setProperty(KAFKACTL_CONFIG, "src/main/resources/config.yml");

        String actual = kafkactlConfig.getConfigDirectory();

        assertEquals("src" + System.getProperty("file.separator") + "main" + System.getProperty("file.separator") + "resources", actual);
    }
}
