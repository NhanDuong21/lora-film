package com.project.paymentservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.providers.vnpay")
public class VnPayProperties {
    private boolean enabled;
    private String tmnCode;
    private String hashSecret;
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getTmnCode() { return tmnCode; }
    public void setTmnCode(String value) { this.tmnCode = value; }
    public String getHashSecret() { return hashSecret; }
    public void setHashSecret(String value) { this.hashSecret = value; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String value) { this.paymentUrl = value; }
    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String value) { this.returnUrl = value; }
    public String getVersion() { return version; }
    public void setVersion(String value) { this.version = value; }
    public String getCommand() { return command; }
    public void setCommand(String value) { this.command = value; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String value) { this.orderType = value; }
}
