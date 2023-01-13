package com.michelin.kafkactl.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@Introspected
public class BearerAccessRefreshToken {
    private String username;
    private Collection<String> roles;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;
}
