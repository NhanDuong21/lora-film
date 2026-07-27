package com.project.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
public class LoginHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "login_time", updatable = false)
    private LocalDateTime loginTime;

    @Column(name = "status", length = 20)
    private String status;
    public Long getId() {
        return this.id;
    }
    public Account getAccount() {
        return this.account;
    }
    public String getIpAddress() {
        return this.ipAddress;
    }
    public String getUserAgent() {
        return this.userAgent;
    }
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }
    public String getStatus() {
        return this.status;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setAccount(Account account) {
        this.account = account;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public LoginHistory() {
    }
    public LoginHistory(Long id, Account account, String ipAddress, String userAgent, LocalDateTime loginTime, String status) {
        this.id = id;
        this.account = account;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.loginTime = loginTime;
        this.status = status;
    }
    public static LoginHistoryBuilder builder() {
        return new LoginHistoryBuilder();
    }
    public static class LoginHistoryBuilder {
        private Long id;
        private Account account;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime loginTime;
        private String status;
        LoginHistoryBuilder() {}
        public LoginHistoryBuilder id(Long id) {
            this.id = id;
            return this;
        }
        public LoginHistoryBuilder account(Account account) {
            this.account = account;
            return this;
        }
        public LoginHistoryBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        public LoginHistoryBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        public LoginHistoryBuilder loginTime(LocalDateTime loginTime) {
            this.loginTime = loginTime;
            return this;
        }
        public LoginHistoryBuilder status(String status) {
            this.status = status;
            return this;
        }
        public LoginHistory build() {
            return new LoginHistory(this.id, this.account, this.ipAddress, this.userAgent, this.loginTime, this.status);
        }
    }
}
