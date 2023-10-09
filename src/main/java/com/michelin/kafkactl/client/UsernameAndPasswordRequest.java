package com.michelin.kafkactl.client;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Username and password request.
 */
@Getter
@Setter
@Builder
@ReflectiveAccess
public class UsernameAndPasswordRequest {
    private String username;
    private String password;
}


