package com.project.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.providers.momo")
public class MomoProperties {
    private boolean enabled;
    private String partnerCode;
    private String accessKey;
    private String secretKey;
    private String createUrl = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String queryUrl = "https://test-payment.momo.vn/v2/gateway/api/query";
    private String redirectUrl;
    private String ipnUrl;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getPartnerCode() { return partnerCode; }
    public void setPartnerCode(String value) { this.partnerCode = value; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String value) { this.accessKey = value; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String value) { this.secretKey = value; }
    public String getCreateUrl() { return createUrl; }
    public void setCreateUrl(String value) { this.createUrl = value; }
    public String getQueryUrl() { return queryUrl; }
    public void setQueryUrl(String value) { this.queryUrl = value; }
    public String getRedirectUrl() { return redirectUrl; }
    public void setRedirectUrl(String value) { this.redirectUrl = value; }
    public String getIpnUrl() { return ipnUrl; }
    public void setIpnUrl(String value) { this.ipnUrl = value; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int value) { this.connectTimeoutMillis = value; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int value) { this.readTimeoutMillis = value; }
}
