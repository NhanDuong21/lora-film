const DEFAULT_MESSAGE = 'Đã có lỗi xảy ra. Vui lòng thử lại.';

const COMMON_ERROR_MESSAGES = {
  ERR_NETWORK: 'Không thể kết nối đến hệ thống. Vui lòng kiểm tra mạng và thử lại.',
  ECONNABORTED: 'Kết nối mất quá nhiều thời gian. Vui lòng thử lại.',
  NETWORK_ERROR: 'Không thể kết nối đến hệ thống. Vui lòng kiểm tra mạng và thử lại.',
  UNAUTHORIZED: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  FORBIDDEN: 'Bạn không có quyền thực hiện thao tác này.',
  NOT_FOUND: 'Không tìm thấy thông tin được yêu cầu.',
  INTERNAL_SERVER_ERROR: 'Hệ thống đang bận. Vui lòng thử lại sau.'
};

const VIETNAMESE_CHARACTER_PATTERN =
  /[ăâđêôơưáàảãạấầẩẫậắằẳẵặéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ]/i;

const getPayload = error => error?.response?.data || error?.data || error || {};

export const getCustomerErrorCode = error => {
  const payload = getPayload(error);
  return payload?.errorCode || payload?.code || error?.code || null;
};

const getHttpStatusMessage = error => {
  const status = error?.response?.status || error?.status;
  if (status === 401) return COMMON_ERROR_MESSAGES.UNAUTHORIZED;
  if (status === 403) return COMMON_ERROR_MESSAGES.FORBIDDEN;
  if (status === 404) return COMMON_ERROR_MESSAGES.NOT_FOUND;
  if (status >= 500) return COMMON_ERROR_MESSAGES.INTERNAL_SERVER_ERROR;
  return null;
};

const getLocalizedRawMessage = error => {
  const payload = getPayload(error);
  const candidates = [payload?.message, payload?.detail, error?.message];
  return candidates.find(value =>
    typeof value === 'string' && VIETNAMESE_CHARACTER_PATTERN.test(value)
  );
};

/**
 * Resolve a customer-safe Vietnamese message without exposing raw backend,
 * Axios, database, or service messages written in English.
 */
export const getCustomerErrorMessage = (
  error,
  fallback = DEFAULT_MESSAGE,
  domainMessages = {}
) => {
  const code = getCustomerErrorCode(error);
  return domainMessages[code]
    || COMMON_ERROR_MESSAGES[code]
    || getHttpStatusMessage(error)
    || getLocalizedRawMessage(error)
    || fallback;
};
