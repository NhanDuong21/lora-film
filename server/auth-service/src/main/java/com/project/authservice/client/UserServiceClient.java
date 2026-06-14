package com.project.authservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.project.authservice.exception.CccdException.CccdAlreadyExistsException;
import com.project.authservice.exception.PhoneAlreadyExistsException;
import com.project.authservice.exception.UserProfileCreateFailedException;

@Component
public class UserServiceClient {
    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${user.service.url}")
    private String userServiceUrl;

    public UserServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Propagates profile creation to User Service.
     * Maps conflicts to specific exception classes or throws UserProfileCreateFailedException.
     *
     * @param request profile creation details
     */
    public void createUserProfile(UserProfileRequest request) {
        String url = userServiceUrl + "/internal/users";
        log.info("Sending profile creation request to User Service: url={}, accountId={}", url, request.getAccountId());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<UserProfileRequest> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);
            log.info("User Service successfully created profile for accountId={}", request.getAccountId());
        } catch (HttpClientErrorException.Conflict e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn("User Service conflict occurred on profile creation: {}", responseBody);
            if (responseBody.contains("USER_PHONE_ALREADY_EXISTS") || responseBody.toLowerCase().contains("phone")) {
                throw new PhoneAlreadyExistsException();
            } else if (responseBody.contains("USER_CCCD_ALREADY_EXISTS") || responseBody.toLowerCase().contains("cccd")) {
                throw new CccdAlreadyExistsException();
            } else {
                log.error("Unknown conflict returned by User Service: {}", responseBody);
                throw new UserProfileCreateFailedException();
            }
        } catch (Exception e) {
            log.error("Failed to propagate profile creation to User Service: {}", e.getMessage());
            throw new UserProfileCreateFailedException();
        }
    }
// public void createUserProfile(UserProfileRequest request) {

//     log.info("======================================");
//     log.info("FAKE USER SERVICE");
//     log.info("AccountId    : {}", request.getAccountId());
//     log.info("FullName     : {}", request.getFullName());
//     log.info("PhoneNumber  : {}", request.getPhoneNumber());
//     log.info("CCCD Masked  : {}", request.getCccdMasked());
//     log.info("Province     : {}", request.getProvinceName());
//     log.info("Gender       : {}", request.getGender());
//     log.info("BirthYear    : {}", request.getBirthYear());
//     log.info("======================================");

//     // Giả lập tạo profile thành công
//     return;
// }
    public static class UserProfileRequest {
        private Long accountId;
        private String fullName;
        private String phoneNumber;
        private String cccd;
        private String cccdMasked;
        private String provinceCode;
        private String provinceName;
        private String gender;
        private String birthday;
        private int birthYear;

        public UserProfileRequest() {}

        public UserProfileRequest(Long accountId, String fullName, String phoneNumber, String cccd, String cccdMasked,
                                  String provinceCode, String provinceName, String gender, String birthday, int birthYear) {
            this.accountId = accountId;
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
            this.cccd = cccd;
            this.cccdMasked = cccdMasked;
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
            this.gender = gender;
            this.birthday = birthday;
            this.birthYear = birthYear;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }

        public String getCccd() {
            return cccd;
        }

        public void setCccd(String cccd) {
            this.cccd = cccd;
        }

        public String getCccdMasked() {
            return cccdMasked;
        }

        public void setCccdMasked(String cccdMasked) {
            this.cccdMasked = cccdMasked;
        }

        public String getProvinceCode() {
            return provinceCode;
        }

        public void setProvinceCode(String provinceCode) {
            this.provinceCode = provinceCode;
        }

        public String getProvinceName() {
            return provinceName;
        }

        public void setProvinceName(String provinceName) {
            this.provinceName = provinceName;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getBirthday() {
            return birthday;
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(int birthYear) {
            this.birthYear = birthYear;
        }
    }
}
