package com.project.paymentservice.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "payment.providers.vnpay")
@Validated
public class VnPayProperties {
    private boolean enabled;
    private String tmnCode;
    private String hashSecret;
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String queryUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String queryIpAddress = "127.0.0.1";
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 10000;
    @Min(30)
    private int queryRetryDelaySeconds = 305;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String value) { this.tmnCode = value; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String value) { this.hashSecret = value; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String value) { this.paymentUrl = value; }
    public String getQueryUrl() { return queryUrl; }
    public void setQueryUrl(String value) { this.queryUrl = value; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String value) { this.returnUrl = value; }
    public String getVersion() { return version; }
    public void setVersion(String value) { this.version = value; }
    public String getCommand() { return command; }
    public void setCommand(String value) { this.command = value; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String value) { this.orderType = value; }
    public String getQueryIpAddress() { return queryIpAddress; }
    public void setQueryIpAddress(String value) { this.queryIpAddress = value; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int value) { this.connectTimeoutMillis = value; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int value) { this.readTimeoutMillis = value; }
    public int getQueryRetryDelaySeconds() { return queryRetryDelaySeconds; }
    public void setQueryRetryDelaySeconds(int value) { this.queryRetryDelaySeconds = value; }
}
