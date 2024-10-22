package com.michelin.kafkactl.client;

import com.michelin.kafkactl.client.predicates.RetryTimeoutPredicate;
import com.michelin.kafkactl.model.Resource;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.retry.annotation.Retryable;
import java.util.List;
import java.util.Map;

/**
 * Namespaced resource client.
 */
@Client("${kafkactl.api}/api/namespaces/")
public interface NamespacedResourceClient {
    /**
     * Delete a given resource.
     *
     * @param namespace    The namespace
     * @param kind         The kind of resource
     * @param name         The name of the resource
     * @param token        The auth token
     * @param version      The version of the resource, for schemas only.
     * @param dryrun       Is dry-run mode or not?
     * @return The delete response
     */
    @Delete("{namespace}/{kind}/{?name,version,dryrun}")
    @Retryable(delay = "${kafkactl.retry.delete.delay}",
        attempts = "${kafkactl.retry.delete.attempt}",
        multiplier = "${kafkactl.retry.delete.multiplier}",
        predicate = RetryTimeoutPredicate.class)
    HttpResponse<List<Resource>> delete(
        String namespace,
        String kind,
        @Header("Authorization") String token,
        @QueryValue String name,
        @Nullable @QueryValue String version,
        @QueryValue boolean dryrun);

    /**
     * Apply a given resource.
     *
     * @param namespace The namespace
     * @param kind      The kind of resource
     * @param token     The auth token
     * @param resource  The resource to apply
     * @param dryrun    Is dry-run mode or not?
     * @return The resource
     */
    @Post("{namespace}/{kind}{?dryrun}")
    @Retryable(delay = "${kafkactl.retry.apply.delay}",
        attempts = "${kafkactl.retry.apply.attempt}",
        multiplier = "${kafkactl.retry.apply.multiplier}",
        predicate = RetryTimeoutPredicate.class)
    HttpResponse<Resource> apply(
        String namespace,
        String kind,
        @Header("Authorization") String token,
        @Body Resource resource,
        @QueryValue boolean dryrun);

    /**
     * List all resources.
     *
     * @param namespace The namespace
     * @param kind      The kind of resource
     * @param name      The name of the resource
     * @param token     The auth token
     * @return The list of resources
     */
    @Get("{namespace}/{kind}{?name}")
    List<Resource> list(
        String namespace,
        String kind,
        @Nullable @QueryValue String name,
        @Header("Authorization") String token);

    /**
     * Get a resource.
     *
     * @param namespace    The namespace
     * @param kind         The kind of resource
     * @param resourceName The name of the resource
     * @param token        The auth token
     * @return The resource
     */
    @Get("{namespace}/{kind}/{resourceName}")
    HttpResponse<Resource> get(
        String namespace,
        String kind,
        String resourceName,
        @Header("Authorization") String token);

    /**
     * Imports the unsynchronized given type of resource.
     *
     * @param namespace The namespace
     * @param kind      The kind of resource
     * @param token     The auth token
     * @param dryrun    Is dry-run mode or not?
     * @return The list of imported resources
     */
    @Post("{namespace}/{kind}/_/import{?dryrun}")
    List<Resource> importResources(
        String namespace,
        String kind,
        @Header("Authorization") String token,
        @QueryValue boolean dryrun);

    /**
     * Delete records for a given topic.
     *
     * @param token     The authentication token
     * @param namespace The namespace
     * @param topic     The topic to delete records
     * @param dryrun    Is dry run mode or not?
     * @return The deleted records response
     */
    @Post("{namespace}/topics/{topic}/delete-records{?dryrun}")
    List<Resource> deleteRecords(
        @Header("Authorization") String token,
        String namespace,
        String topic,
        @QueryValue boolean dryrun);

    /**
     * Reset offsets for a given topic and consumer group.
     *
     * @param token             The authentication token
     * @param namespace         The namespace
     * @param consumerGroupName The consumer group
     * @param json              The information about how to reset
     * @param dryrun            Is dry run mode or not?
     * @return The reset offsets response
     */
    @Post("{namespace}/consumer-groups/{consumerGroupName}/reset{?dryrun}")
    List<Resource> resetOffsets(
        @Header("Authorization") String token,
        String namespace,
        String consumerGroupName,
        @Body Resource json,
        @QueryValue boolean dryrun);

    /**
     * Change the state of a given connector.
     *
     * @param namespace            The namespace
     * @param connector            The connector to change
     * @param changeConnectorState The state
     * @param token                The auth token
     * @return The change state response
     */
    @Post("{namespace}/connectors/{connector}/change-state")
    HttpResponse<Resource> changeConnectorState(
        String namespace,
        String connector,
        @Body Resource changeConnectorState,
        @Header("Authorization") String token);

    /**
     * Change the schema compatibility mode.
     *
     * @param namespace     The namespace
     * @param subject       The subject
     * @param compatibility The compatibility to apply
     * @param token         The auth token
     * @return The change compatibility response
     */
    @Post("{namespace}/schemas/{subject}/config")
    HttpResponse<Resource> changeSchemaCompatibility(
        String namespace,
        String subject,
        @Body Map<String, String> compatibility,
        @Header("Authorization") String token);

    /**
     * Reset password of a given user.
     *
     * @param namespace The namespace
     * @param user      The user
     * @param token     The auth token
     * @return The reset password response
     */
    @Post("{namespace}/users/{user}/reset-password")
    HttpResponse<Resource> resetPassword(String namespace, String user, @Header("Authorization") String token);

    /**
     * List all available connect clusters for vaulting.
     *
     * @param namespace The namespace
     * @return The list of connect clusters
     */
    @Get("{namespace}/connect-clusters/_/vaults")
    List<Resource> listAvailableVaultsConnectClusters(
        String namespace,
        @Header("Authorization") String token);

    /**
     * Vault a secret for a specific Kafka Connect Cluster.
     *
     * @param namespace      The namespace
     * @param connectCluster The Kafka connect cluster to use for vaulting secret.
     * @param passwords      The list of passwords to encrypt.
     * @return The list of VaultResult.
     */
    @Post("{namespace}/connect-clusters/{connectCluster}/vaults")
    List<Resource> vaultsOnConnectClusters(
        final String namespace,
        final String connectCluster,
        @Body List<String> passwords,
        @Header("Authorization") String token);
}
