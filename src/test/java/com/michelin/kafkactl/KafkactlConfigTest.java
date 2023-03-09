package com.michelin.kafkactl;

import com.michelin.kafkactl.services.SystemService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static com.michelin.kafkactl.KafkactlConfig.KAFKACTL_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KafkactlConfigTest {
    private final KafkactlConfig kafkactlConfig = new KafkactlConfig();

    @Test
    void shouldGetConfigDirectoryFromUserHome() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("");
            when(SystemService.getProperty(any()))
                    .thenAnswer(answer -> System.getProperty(answer.getArgument(0)));

            String actual = kafkactlConfig.getConfigDirectory();
            assertEquals(System.getProperty("user.home") + "/.kafkactl", actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfig() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("src/main/resources/config.yml");

            String actual = kafkactlConfig.getConfigDirectory();
            assertEquals("src" + System.getProperty("file.separator") + "main" + System.getProperty("file.separator") + "resources",
                    actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfigNoParent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("config.yml");

            String actual = kafkactlConfig.getConfigDirectory();
            assertEquals(".", actual);
        }
    }

    @Test
    void shouldGetConfigDirectoryFromKafkactlConfigParentIsCurrent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("./config.yml");

            String actual = kafkactlConfig.getConfigDirectory();
            assertEquals(".", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromUserHome() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("");
            when(SystemService.getProperty(any()))
                    .thenAnswer(answer -> System.getProperty(answer.getArgument(0)));

            String actual = kafkactlConfig.getConfigPath();
            assertEquals(System.getProperty("user.home") + "/.kafkactl/config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfig() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("src/main/resources/config.yml");

            String actual = kafkactlConfig.getConfigPath();
            assertEquals("src/main/resources/config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfigNoParent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("config.yml");

            String actual = kafkactlConfig.getConfigPath();
            assertEquals("config.yml", actual);
        }
    }

    @Test
    void shouldGetConfigPathFromKafkactlConfigParentIsCurrent() {
        try (MockedStatic<SystemService> mocked = mockStatic(SystemService.class)) {
            mocked.when(() -> SystemService.getEnv(KAFKACTL_CONFIG))
                    .thenReturn("./config.yml");

            String actual = kafkactlConfig.getConfigPath();
            assertEquals("./config.yml", actual);
        }
    }
}
