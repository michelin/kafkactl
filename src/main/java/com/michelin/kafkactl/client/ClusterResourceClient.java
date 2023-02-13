package com.michelin.kafkactl.client;

import com.michelin.kafkactl.models.ApiResource;
import com.michelin.kafkactl.models.Resource;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;

import java.util.List;

@Client("${kafkactl.api}")
public interface ClusterResourceClient {
    @Post("/login")
    BearerAccessRefreshToken login(@Body UsernameAndPasswordRequest request);

    @Get("/token_info")
    UserInfoResponse tokenInfo(@Header("Authorization") String token);

    @Get("/api-resources")
    List<ApiResource> listResourceDefinitions(@Header("Authorization") String token);

    @Delete("/api/{kind}/{resource}{?dryrun}")
    HttpResponse<Void> delete(@Header("Authorization") String token, String kind, String resource, @QueryValue boolean dryrun);

    @Post("/api/{kind}{?dryrun}")
    HttpResponse<Resource> apply(@Header("Authorization") String token, String kind, @Body Resource json, @QueryValue boolean dryrun);

    @Get("/api/{kind}")
    List<Resource> list(@Header("Authorization") String token, String kind);

    @Get("/api/{kind}/{resource}")
    HttpResponse<Resource> get(@Header("Authorization") String token, String kind, String resource);
}
