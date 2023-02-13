package com.michelin.kafkactl.client;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ReflectiveAccess
public class UserInfoResponse {
    private boolean active;
    private String username;
    private long exp;
}
