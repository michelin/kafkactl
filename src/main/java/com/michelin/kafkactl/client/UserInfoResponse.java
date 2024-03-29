package com.michelin.kafkactl.client;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import lombok.Setter;

/**
 * User info response.
 */
@Getter
@Setter
@ReflectiveAccess
public class UserInfoResponse {
    private boolean active;
    private String username;
    private long exp;
}
