package com.michelin.kafkactl.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.michelin.kafkactl.KafkactlCommand;
import com.michelin.kafkactl.KafkactlConfig;
import com.michelin.kafkactl.client.BearerAccessRefreshToken;
import com.michelin.kafkactl.client.ClusterResourceClient;
import com.michelin.kafkactl.client.UserInfoResponse;
import com.michelin.kafkactl.client.UsernameAndPasswordRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@Singleton
public class LoginService {
    private static final String UNEXPECTED_ERROR = "Unexpected error occurred: ";
    private final KafkactlConfig kafkactlConfig;
    private final ClusterResourceClient clusterResourceClient;
    private final File jwtFile;
    private String accessToken = null;

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
     * @return true if the user is authenticated, false otherwise
     */
    public boolean doAuthenticate() {
        return isAuthenticated() || login("gitlab", kafkactlConfig.getUserToken());
    }

    /**
     * Is the user authenticated already
     * @return true if he is, false otherwise
     */
    public boolean isAuthenticated() {
        try {
            if (!jwtFile.exists()) {
                return false;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            BearerAccessRefreshToken token = objectMapper.readValue(jwtFile, BearerAccessRefreshToken.class);
            UserInfoResponse userInfo = clusterResourceClient.tokenInfo("Bearer " + token.getAccessToken());
            if (KafkactlCommand.VERBOSE) {
                Date expiry = new Date(userInfo.getExp() * 1000);
                System.out.println("Authentication reused, welcome " + userInfo.getUsername() + "!");
                System.out.println("Your session is valid until " + expiry + ".");
            }

            accessToken = token.getAccessToken();
            return userInfo.isActive();
        } catch (IOException e) {
            System.out.println(UNEXPECTED_ERROR + e.getMessage());
        } catch (HttpClientResponseException e) {
            if (e.getStatus() != HttpStatus.UNAUTHORIZED) {
                System.out.println(UNEXPECTED_ERROR + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Authenticate the user
     * @param user The user
     * @param password The password
     * @return true if he is authenticated, false otherwise
     */
    public boolean login(String user, String password) {
        try {
            if (KafkactlCommand.VERBOSE) {
                System.out.println("Authenticating...");
            }

            BearerAccessRefreshToken tokenResponse = clusterResourceClient.login(UsernameAndPasswordRequest
                    .builder()
                    .username(user)
                    .password(password)
                    .build());

            accessToken = tokenResponse.getAccessToken();

            if (KafkactlCommand.VERBOSE) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, tokenResponse.getExpiresIn());
                System.out.println("Authentication successful, welcome " + tokenResponse.getUsername() + "!");
                System.out.println("Your session is valid until " + calendar.getTime() + ".");
            }

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writeValue(jwtFile, tokenResponse);
            } catch (IOException e) {
                System.out.println(UNEXPECTED_ERROR + e.getMessage());
            }

            return true;
        } catch (HttpClientResponseException e) {
            System.out.println("Authentication failed with message: " + e.getMessage());
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
