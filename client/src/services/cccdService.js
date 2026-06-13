import axios from "axios";

const CCCD_API_URL = import.meta.env.VITE_CCCD_API_URL;
const CCCD_API_KEY = import.meta.env.VITE_CCCD_API_KEY;

export const checkCCCD = async (cccdValue) => {
    try {
        const response = await axios.post(
            CCCD_API_URL,
            { cccd: cccdValue },
            {
                headers: {
                    "Content-Type": "application/json",
                    "x-api-key": CCCD_API_KEY
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