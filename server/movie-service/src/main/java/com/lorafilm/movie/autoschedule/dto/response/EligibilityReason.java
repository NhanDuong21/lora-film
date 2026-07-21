package com.lorafilm.movie.autoschedule.dto.response;

public class EligibilityReason {
    private String code;
    private String message;

    public EligibilityReason() {}

    public EligibilityReason(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
