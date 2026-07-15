export const parseApiError = (err) => {
  const d = err?.response?.data || err;
  if (!d) return err?.message || 'Lỗi không xác định.';

  if (d.errorCode === 'INVALID_ENUM_VALUE') {
    const field = d.data?.field || d.field || 'không xác định';
    const val = d.data?.rejectedValue ?? d.data?.value ?? '';
    const allowed = (d.data?.allowedValues || []).join(', ');
    return `Giá trị "${val}" không hợp lệ cho trường "${field}". Giá trị hợp lệ: ${allowed}.`;
  }
  if (d.errorCode === 'VALIDATION_ERROR' && d.data?.fieldErrors) {
    return d.data.fieldErrors.map(e => `"${e.field}": ${e.message}`).join('\n');
  }
  if (d.message) return d.message;
  return err?.message || 'Lỗi không xác định.';
};
