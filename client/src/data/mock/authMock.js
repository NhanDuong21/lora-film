export const mockRegister = async (userData) => {
    // Return a delayed response matching the backend contract structure (1 second delay)
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Simulate backend contract errors based on specific email/field values for developer testing
    if (userData.email === "exist@example.com") {
        throw {
            success: false,
            code: "AUTH_EMAIL_ALREADY_EXISTS",
            message: "Email already exists"
        };
    }
    if (userData.phoneNumber === "0900000000") {
        throw {
            success: false,
            code: "USER_PHONE_ALREADY_EXISTS",
            message: "Phone number already exists"
        };
    }
    if (userData.cccd === "999999999999") {
        throw {
            success: false,
            code: "USER_CCCD_ALREADY_EXISTS",
            message: "CCCD already exists"
        };
    }
    if (userData.cccd === "111111111111") {
        throw {
            success: false,
            code: "USER_CCCD_INVALID",
            message: "CCCD invalid"
        };
    }
    if (userData.email === "mismatch@example.com") {
        throw {
            success: false,
            code: "USER_BIRTHDAY_CCCD_MISMATCH",
            message: "Birthday and CCCD mismatch"
        };
    }
    if (userData.email === "validation@example.com") {
        throw {
            success: false,
            code: "VALIDATION_ERROR",
            message: "Validation failed"
        };
    }
    if (userData.email === "profile@example.com") {
        throw {
            success: false,
            code: "USER_PROFILE_CREATE_FAILED",
            message: "Profile creation failed"
        };
    }
    if (userData.email === "server@example.com") {
        throw {
            success: false,
            code: "INTERNAL_SERVER_ERROR",
            message: "Internal server error"
        };
    }

    // Default successful register mock response
    return {
        success: true,
        message: "Register successfully",
        data: {
            accountId: 1,
            email: userData.email || "user@example.com",
            role: "CUSTOMER",
            fullName: userData.fullName || "Nguyen Van A",
            phoneNumber: userData.phoneNumber || "0901234567",
            cccdMasked: userData.cccd ? `${userData.cccd.substring(0, 3)}******${userData.cccd.substring(9, 12)}` : "092******789",
            provinceName: "Cần Thơ",
            gender: "MALE",
            birthYear: userData.birthday ? parseInt(userData.birthday.split("-")[0], 10) : 2005
        }
    };
};
