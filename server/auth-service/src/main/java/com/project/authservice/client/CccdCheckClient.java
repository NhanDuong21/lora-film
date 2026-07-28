package com.project.authservice.client;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.project.authservice.exception.CccdException.InvalidCccdException;

@Component
public class CccdCheckClient {
    private static final Logger log = LoggerFactory.getLogger(CccdCheckClient.class);

    private final RestTemplate restTemplate;

    @Value("${cccd.api.url}")
    private String cccdApiUrl;

    @Value("${cccd.api.key}")
    private String cccdApiKey;

    public CccdCheckClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Checks CCCD and returns derived info. Falls back to local validation if external API fails.
     * Never logs full CCCD values.
     *
     * @param cccd CCCD number
     * @return CCCD check result
     */
    public CccdInfo checkCccd(String cccd) {
        if (cccd == null || cccd.length() != 12) {
            throw new InvalidCccdException();
        }

        String maskedCccd = cccd.substring(0, 3) + "******" + cccd.substring(9);
        log.info("Checking CCCD for: {}", maskedCccd);

        // Try calling the remote service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", cccdApiKey);

            Map<String, String> body = new HashMap<>();
            body.put("cccd", cccd);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<RemoteCccdResponse> response = restTemplate.postForEntity(cccdApiUrl, request, RemoteCccdResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                RemoteCccdResponse resBody = response.getBody();
                if (Boolean.TRUE.equals(resBody.getValid())) {
                    log.info("CCCD API verification successful for {}", maskedCccd);
                    return new CccdInfo(
                        resBody.getCccdMasked(),
                        resBody.getProvinceCode(),
                        resBody.getProvinceName(),
                        resBody.getGender(),
                        resBody.getBirthYear(),
                        resBody.getNote()
                    );
                } else {
                    log.warn("CCCD API returned invalid format for {}", maskedCccd);
                    throw new InvalidCccdException();
                }
            }
        } catch (InvalidCccdException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to CCCD Check API: {}", e.getMessage());
            throw new InvalidCccdException();
        }

            throw new InvalidCccdException();
    }

    

    public static class CccdInfo {
        private String cccdMasked;
        private String provinceCode;
        private String provinceName;
        private String gender;
        private int birthYear;
        private String note;

        public CccdInfo() {}

        public CccdInfo(String cccdMasked, String provinceCode, String provinceName, String gender, int birthYear, String note) {
            this.cccdMasked = cccdMasked;
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
            this.gender = gender;
            this.birthYear = birthYear;
            this.note = note;
        }

        public String getCccdMasked() {
            return cccdMasked;
        }

        public String getProvinceCode() {
            return provinceCode;
        }

        public String getProvinceName() {
            return provinceName;
        }

        public String getGender() {
            return gender;
        }

        public int getBirthYear() {
            return birthYear;
        }

        public String getNote() {
            return note;
        }
    }

    private static class RemoteCccdResponse {
        private Boolean valid;
        private String cccdMasked;
        private String provinceCode;
        private String provinceName;
        private String gender;
        private Integer birthYear;
        private String note;

        public RemoteCccdResponse() {}

        public Boolean getValid() {
            return valid;
        }

        public void setValid(Boolean valid) {
            this.valid = valid;
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

        public Integer getBirthYear() {
            return birthYear;
        }

        public void setBirthYear(Integer birthYear) {
            this.birthYear = birthYear;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
