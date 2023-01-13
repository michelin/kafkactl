package com.michelin.kafkactl.client;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Introspected
public class UsernameAndPasswordRequest {
    private String username;
    private String password;
}


