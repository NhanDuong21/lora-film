import axios from "axios";

export const checkCCCD = async (cccdValue) => {
    try {
        const response = await axios.post(
            "https://api-check-cccd.lorafilm.xyz/api/cccd/check",
            { cccd: cccdValue },
            {
                headers: {
                    "Content-Type": "application/json",
                    "x-api-key": "lora_cccd_2026_secret"
                }
            }
        );
        return response.data;
    } catch (error) {
        if (error.response && error.response.data) {
            throw error.response.data;
        }
        throw new Error("Không thể kết nối với dịch vụ xác thực CCCD.", { cause: error });
    }
};
