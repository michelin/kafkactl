package com.michelin.kafkactl.client;

import com.michelin.kafkactl.model.ApiResource;
import com.michelin.kafkactl.model.Resource;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import java.util.List;

/**
 * Cluster resource client.
 */
@Client("${kafkactl.api}")
public interface ClusterResourceClient {
    @Post("/login")
    BearerAccessRefreshToken login(@Body UsernameAndPasswordRequest request);

    @Get("/token_info")
    UserInfoResponse tokenInfo(@Header("Authorization") String token);

    @Get("/api-resources")
    List<ApiResource> listResourceDefinitions(@Header("Authorization") String token);

    @Delete("/api/{kind}/{resource}{?dryrun}")
    HttpResponse<Void> delete(@Header("Authorization") String token, String kind, String resource,
                              @QueryValue boolean dryrun);

    @Post("/api/{kind}{?dryrun}")
    HttpResponse<Resource> apply(@Header("Authorization") String token, String kind, @Body Resource json,
                                 @QueryValue boolean dryrun);

    @Get("/api/{kind}")
    List<Resource> list(@Header("Authorization") String token, String kind);

    @Get("/api/{kind}/{resource}")
    HttpResponse<Resource> get(@Header("Authorization") String token, String kind, String resource);
}
