export const normalizeApiError = (err) => {
  const responseData = err?.response?.data || err?.data || err;
  const status = err?.response?.status || responseData?.status || 500;
  
  const normalized = {
    status,
    code: responseData?.errorCode || responseData?.code || 'UNKNOWN_ERROR',
    message: responseData?.message || responseData?.errorMessage || err?.message || 'Đã xảy ra lỗi không xác định',
    fieldErrors: {},
    raw: err
  };

  // Handle Spring Boot validation fieldErrors if present
  const rawFieldErrors = responseData?.data?.fieldErrors || responseData?.fieldErrors || [];
  if (Array.isArray(rawFieldErrors)) {
    rawFieldErrors.forEach(e => {
      if (e.field && e.message) {
        normalized.fieldErrors[e.field] = e.message;
      }
    });
  }

  // Handle INVALID_ENUM_VALUE specifically if returned by backend
  if (normalized.code === 'INVALID_ENUM_VALUE') {
    const field = responseData?.data?.field || responseData?.field || 'không xác định';
    const val = responseData?.data?.rejectedValue ?? responseData?.data?.value ?? '';
    const allowed = (responseData?.data?.allowedValues || []).join(', ');
    normalized.message = `Giá trị "${val}" không hợp lệ cho trường "${field}". Giá trị hợp lệ: ${allowed}.`;
  }

  // Handle Axios Network Error
  if (err?.code === 'ERR_NETWORK') {
    normalized.message = 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng.';
  }

  return normalized;
};

export const getErrorMessage = (err, fallback = 'Lỗi không xác định') => {
  const normalized = normalizeApiError(err);
  return normalized.message || fallback;
};

export const getFieldErrors = (err) => {
  const normalized = normalizeApiError(err);
  return normalized.fieldErrors;
};

export const isConflictError = (err) => {
  const normalized = normalizeApiError(err);
  return normalized.status === 409 || normalized.code === 'CONFLICT';
};

// Keep old parseApiError for backward compatibility during transition
export const parseApiError = (err) => {
  return getErrorMessage(err);
};
