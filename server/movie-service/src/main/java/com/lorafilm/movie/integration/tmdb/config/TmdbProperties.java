package com.lorafilm.movie.integration.tmdb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {
    private String baseUrl;
    private String apiKey;
    private int batchSize = 20;
    private boolean integrationEnabled = true;
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 30;

    private int syncStaleThresholdSeconds = 300; // default 5 minutes

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isIntegrationEnabled() {
        return integrationEnabled;
    }

    public void setIntegrationEnabled(boolean integrationEnabled) {
        this.integrationEnabled = integrationEnabled;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public int getSyncStaleThresholdSeconds() {
        return syncStaleThresholdSeconds;
    }

    public void setSyncStaleThresholdSeconds(int syncStaleThresholdSeconds) {
        this.syncStaleThresholdSeconds = syncStaleThresholdSeconds;
    }
}
