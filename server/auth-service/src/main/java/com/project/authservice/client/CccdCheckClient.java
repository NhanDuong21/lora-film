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

    @Value("${cccd.api.url:https://api-check-cccd.lorafilm.xyz/api/cccd/check}")
    private String cccdApiUrl;

    @Value("${cccd.api.key:lora_cccd_2026_secret}")
    private String cccdApiKey;

    private static final Map<String, String> PROVINCE_MAP = new HashMap<>();
    static {
        PROVINCE_MAP.put("001", "Hà Nội");
        PROVINCE_MAP.put("002", "Hà Giang");
        PROVINCE_MAP.put("004", "Cao Bằng");
        PROVINCE_MAP.put("006", "Bắc Kạn");
        PROVINCE_MAP.put("008", "Tuyên Quang");
        PROVINCE_MAP.put("010", "Lào Cai");
        PROVINCE_MAP.put("011", "Điện Biên");
        PROVINCE_MAP.put("012", "Lai Châu");
        PROVINCE_MAP.put("014", "Sơn La");
        PROVINCE_MAP.put("015", "Yên Bái");
        PROVINCE_MAP.put("017", "Hòa Bình");
        PROVINCE_MAP.put("019", "Thái Nguyên");
        PROVINCE_MAP.put("020", "Lạng Sơn");
        PROVINCE_MAP.put("022", "Quảng Ninh");
        PROVINCE_MAP.put("024", "Bắc Giang");
        PROVINCE_MAP.put("025", "Phú Thọ");
        PROVINCE_MAP.put("026", "Vĩnh Phúc");
        PROVINCE_MAP.put("027", "Bắc Ninh");
        PROVINCE_MAP.put("030", "Hải Dương");
        PROVINCE_MAP.put("031", "Hải Phòng");
        PROVINCE_MAP.put("033", "Hưng Yên");
        PROVINCE_MAP.put("034", "Thái Bình");
        PROVINCE_MAP.put("035", "Hà Nam");
        PROVINCE_MAP.put("036", "Nam Định");
        PROVINCE_MAP.put("037", "Ninh Bình");
        PROVINCE_MAP.put("038", "Thanh Hóa");
        PROVINCE_MAP.put("040", "Nghệ An");
        PROVINCE_MAP.put("042", "Hà Tĩnh");
        PROVINCE_MAP.put("044", "Quảng Bình");
        PROVINCE_MAP.put("045", "Quảng Trị");
        PROVINCE_MAP.put("046", "Thừa Thiên Huế");
        PROVINCE_MAP.put("048", "Đà Nẵng");
        PROVINCE_MAP.put("049", "Quảng Nam");
        PROVINCE_MAP.put("051", "Quảng Ngãi");
        PROVINCE_MAP.put("052", "Bình Định");
        PROVINCE_MAP.put("054", "Phú Yên");
        PROVINCE_MAP.put("056", "Khánh Hòa");
        PROVINCE_MAP.put("058", "Ninh Thuận");
        PROVINCE_MAP.put("060", "Bình Thuận");
        PROVINCE_MAP.put("062", "Kon Tum");
        PROVINCE_MAP.put("064", "Gia Lai");
        PROVINCE_MAP.put("066", "Đắk Lắk");
        PROVINCE_MAP.put("068", "Đắk Nông");
        PROVINCE_MAP.put("070", "Lâm Đồng");
        PROVINCE_MAP.put("072", "Bình Phước");
        PROVINCE_MAP.put("074", "Tây Ninh");
        PROVINCE_MAP.put("075", "Bình Dương");
        PROVINCE_MAP.put("077", "Đồng Nai");
        PROVINCE_MAP.put("079", "Thành phố Hồ Chí Minh");
        PROVINCE_MAP.put("080", "Long An");
        PROVINCE_MAP.put("082", "Tiền Giang");
        PROVINCE_MAP.put("083", "Bến Tre");
        PROVINCE_MAP.put("084", "Trà Vinh");
        PROVINCE_MAP.put("086", "Vĩnh Long");
        PROVINCE_MAP.put("087", "Đồng Tháp");
        PROVINCE_MAP.put("089", "An Giang");
        PROVINCE_MAP.put("091", "Kiên Giang");
        PROVINCE_MAP.put("092", "Cần Thơ");
        PROVINCE_MAP.put("093", "Hậu Giang");
        PROVINCE_MAP.put("094", "Sóc Trăng");
        PROVINCE_MAP.put("095", "Bạc Liêu");
        PROVINCE_MAP.put("096", "Cà Mau");
    }

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
            log.error("Failed to connect to CCCD Check API: {}. Falling back to local validation.", e.getMessage());
        }

        // Local Validation Fallback
        return validateAndDecodeLocally(cccd, maskedCccd);
    }

    private CccdInfo validateAndDecodeLocally(String cccd, String maskedCccd) {
        if (!cccd.matches("\\d{12}")) {
            log.warn("CCCD local validation failed: not 12 digits for {}", maskedCccd);
            throw new InvalidCccdException();
        }

        String provinceCode = cccd.substring(0, 3);
        String provinceName = PROVINCE_MAP.getOrDefault(provinceCode, "Unknown Province");

        char genderChar = cccd.charAt(3);
        if (genderChar < '0' || genderChar > '7') {
            log.warn("CCCD local validation failed: invalid century/gender digit '{}' for {}", genderChar, maskedCccd);
            throw new InvalidCccdException();
        }

        int genderDigit = genderChar - '0';
        String gender = (genderDigit % 2 == 0) ? "MALE" : "FEMALE";

        int centuryPrefix = 19 + (genderDigit / 2);
        String yearSuffix = cccd.substring(4, 6);
        int birthYear;
        try {
            birthYear = (centuryPrefix * 100) + Integer.parseInt(yearSuffix);
        } catch (NumberFormatException e) {
            log.warn("CCCD local validation failed: failed to parse birth year digits '{}' for {}", yearSuffix, maskedCccd);
            throw new InvalidCccdException();
        }

        log.info("CCCD local validation successful. Derived: birthYear={}, gender={}, province={}", birthYear, gender, provinceName);
        return new CccdInfo(
            maskedCccd,
            provinceCode,
            provinceName,
            gender,
            birthYear,
            "Validated via local fallback system."
        );
    }

    public static class CccdInfo {
        private final String cccdMasked;
        private final String provinceCode;
        private final String provinceName;
        private final String gender;
        private final int birthYear;
        private final String note;

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
