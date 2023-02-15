package com.michelin.kafkactl.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.kafkactl.KafkactlConfig;
import com.michelin.kafkactl.client.BearerAccessRefreshToken;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.UserInfoResponse;
import com.michelin.kafkactl.client.UsernameAndPasswordRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@Singleton
public class LoginService {
    private static final String UNEXPECTED_ERROR = "Unexpected error occurred:";
    private final KafkactlConfig kafkactlConfig;
    private final ClusterResourceClient clusterResourceClient;
    private final File jwtFile;
    private String accessToken;

    /**
     * Constructor
     * @param kafkactlConfig The Kafkactl config
     * @param clusterResourceClient The client for resources
     */
    public LoginService(KafkactlConfig kafkactlConfig, ClusterResourceClient clusterResourceClient) {
        this.kafkactlConfig = kafkactlConfig;
        this.clusterResourceClient = clusterResourceClient;
        this.jwtFile = new File(kafkactlConfig.getConfigPath() + "/jwt");
        // Create base kafkactl dir if not exists
        File kafkactlDir = new File(kafkactlConfig.getConfigPath());
        if (!kafkactlDir.exists()) {
            kafkactlDir.mkdir();
        }
    }

    /**
     * Get the authorization header
     * @return The authorization header
     */
    public String getAuthorization() {
        return "Bearer " + accessToken;
    }

    /**
     * Check if the user is authenticated, or authenticate him otherwise
     * @param verbose Is verbose mode activated or not
     * @param commandSpec The command spec used to print the output
     * @return true if the user is authenticated, false otherwise
     */
    public boolean doAuthenticate(CommandLine.Model.CommandSpec commandSpec, boolean verbose) {
        return isAuthenticated(commandSpec, verbose)
                || login(commandSpec, "gitlab", kafkactlConfig.getUserToken(), verbose);
    }

    /**
     * Is the user authenticated already
     * @param verbose Is verbose mode activated or not
     * @param commandSpec The command spec used to print the output
     * @return true if he is, false otherwise
     */
    public boolean isAuthenticated(CommandLine.Model.CommandSpec commandSpec, boolean verbose) {
        try {
            if (!jwtFile.exists()) {
                return false;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            BearerAccessRefreshToken token = objectMapper.readValue(jwtFile, BearerAccessRefreshToken.class);
            UserInfoResponse userInfo = clusterResourceClient.tokenInfo("Bearer " + token.getAccessToken());
            if (verbose) {
                Date expiry = new Date(userInfo.getExp() * 1000);
                commandSpec.commandLine().getOut().println("Authentication reused. Welcome " + userInfo.getUsername() + "!");
                commandSpec.commandLine().getOut().println("Your session is valid until " + expiry + ".");
            }

            accessToken = token.getAccessToken();
            return userInfo.isActive();
        } catch (IOException e) {
            commandSpec.commandLine().getErr().println(String.format("%s %s.",
                    UNEXPECTED_ERROR, e.getMessage()));
        } catch (HttpClientResponseException e) {
            if (e.getStatus() != HttpStatus.UNAUTHORIZED) {
                commandSpec.commandLine().getErr().println(String.format("%s %s (%s).",
                        UNEXPECTED_ERROR, e.getMessage(), e.getStatus().getCode()));
            }
        }
        return false;
    }

    /**
     * Authenticate the user
     * @param user The user
     * @param password The password
     * @param verbose Is verbose mode activated or not
     * @param commandSpec The command spec used to print the output
     * @return true if he is authenticated, false otherwise
     */
    public boolean login(CommandLine.Model.CommandSpec commandSpec, String user, String password, boolean verbose) {
        try {
            if (verbose) {
                commandSpec.commandLine().getOut().println("Authenticating...");
            }

            BearerAccessRefreshToken tokenResponse = clusterResourceClient.login(UsernameAndPasswordRequest
                    .builder()
                    .username(user)
                    .password(password)
                    .build());

            accessToken = tokenResponse.getAccessToken();

            if (verbose) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, tokenResponse.getExpiresIn());
                commandSpec.commandLine().getOut().println("Authentication successful. Welcome " + tokenResponse.getUsername() + "!");
                commandSpec.commandLine().getOut().println("Your session is valid until " + calendar.getTime() + ".");
            }

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(jwtFile, tokenResponse);
            } catch (IOException e) {
                commandSpec.commandLine().getErr().println(String.format("%s %s.",
                        UNEXPECTED_ERROR, e.getMessage()));
            }

            return true;
        } catch (HttpClientResponseException e) {
            commandSpec.commandLine().getErr().println("Authentication failed because " + e.getMessage().toLowerCase() + ".");
        }

        return false;
    }

    /**
     * If exists, delete JWT file
     */
    public void deleteJWTFile() {
        if (jwtFile.exists()) {
            jwtFile.delete();
        }
    }
}
