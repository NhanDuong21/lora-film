import apiClient from '@/services/apiClient';

const unwrap = response => response?.data?.data ?? response?.data;

const ERROR_MESSAGES = {
  PAYMENT_NOT_FOUND: 'Không tìm thấy giao dịch thanh toán.',
  PAYMENT_ORDER_NOT_FOUND: 'Không tìm thấy đơn thanh toán tương ứng.',
  PAYMENT_ACCESS_DENIED: 'Bạn không có quyền xem giao dịch này.',
  PAYMENT_PROVIDER_SESSION_ACTIVE: 'Phiên thanh toán đang hoạt động. Vui lòng hoàn tất hoặc chờ kết quả từ cổng thanh toán.',
  PAYMENT_ATTEMPT_ACTIVE: 'Đơn này đang có một giao dịch thanh toán hoạt động. Vui lòng tiếp tục giao dịch hiện tại.',
  PAYMENT_ACTIVE_ATTEMPT_EXISTS: 'Đơn này đang có một giao dịch thanh toán hoạt động. Vui lòng tiếp tục giao dịch hiện tại.',
  PAYMENT_REQUEST_IN_PROGRESS: 'Yêu cầu thanh toán đang được xử lý. Vui lòng chờ trong giây lát.',
  IDEMPOTENCY_REQUEST_IN_PROGRESS: 'Yêu cầu thanh toán đang được xử lý. Vui lòng chờ trong giây lát.',
  PAYMENT_ALREADY_SUCCESS: 'Giao dịch này đã thanh toán thành công.',
  BOOKING_ALREADY_PAID: 'Đơn đặt vé này đã được thanh toán.',
  BOOKING_CANCELLED: 'Đơn đặt vé đã được hủy và ghế đã được trả lại. Vui lòng tạo đơn mới nếu bạn muốn tiếp tục.',
  PAYMENT_EXPIRED: 'Đơn đã hết thời gian thanh toán.',
  BOOKING_NOT_PAYABLE: 'Đơn hiện không thể thanh toán. Vui lòng kiểm tra lại trạng thái đơn.',
  BOOKING_PAYMENT_DEADLINE_EXPIRED: 'Đơn đã hết thời gian giữ ghế.',
  BOOKING_AMOUNT_NOT_LOCKED: 'Đơn chưa được chốt số tiền thanh toán. Vui lòng tải lại trang checkout.',
  BOOKING_SEATS_NOT_HELD: 'Ghế của đơn không còn được giữ. Vui lòng chọn lại ghế.',
  BOOKING_SERVICE_UNAVAILABLE: 'Chưa thể kiểm tra đơn đặt vé lúc này. Vui lòng thử lại sau.',
  CASH_AMOUNT_INSUFFICIENT: 'Số tiền khách đưa chưa đủ để thanh toán.',
  PAYMENT_NOT_CASH: 'Giao dịch này không phải giao dịch tiền mặt.',
  PAYMENT_INVALID_STATE: 'Trạng thái giao dịch hiện tại không cho phép thực hiện thao tác này.',
  PAYMENT_PROVIDER_INVALID: 'Phương thức thanh toán không hợp lệ.',
  PAYMENT_PROVIDER_UNAVAILABLE: 'Cổng thanh toán tạm thời chưa sẵn sàng. Vui lòng thử lại sau.',
  PAYMENT_SESSION_CREATION_FAILED: 'Không thể mở phiên thanh toán. Vui lòng thử lại sau.',
  MOMO_SESSION_REJECTED: 'MoMo chưa thể tạo phiên thanh toán. Vui lòng thử lại hoặc chọn phương thức khác.',
  MOMO_AMOUNT_OUT_OF_RANGE: 'Số tiền của đơn nằm ngoài giới hạn thanh toán của MoMo.',
  PAYMENT_AMOUNT_MISMATCH: 'Số tiền từ cổng thanh toán không khớp với đơn. Giao dịch đã được chuyển sang đối soát.',
  IDEMPOTENCY_CONFLICT: 'Yêu cầu này đã được dùng cho một thao tác khác. Vui lòng thử lại.',
  IDEMPOTENCY_KEY_REUSED: 'Yêu cầu này đã được dùng cho một thao tác khác. Vui lòng thử lại.',
  PROVIDER_EVENT_CONFLICT: 'Kết quả từ cổng thanh toán có mâu thuẫn và đang được đối soát.',
  PAYMENT_RECONCILIATION_REQUIRED: 'Giao dịch đang cần đối soát. Nhân viên sẽ kiểm tra kết quả thanh toán.',
  WEBHOOK_SIGNATURE_INVALID: 'Thông báo từ cổng thanh toán không hợp lệ nên không thể xử lý lại.',
  PAYMENT_NOT_REFUNDABLE: 'Chỉ giao dịch đã thanh toán thành công mới có thể hoàn tiền.',
  REFUND_NOT_FOUND: 'Không tìm thấy yêu cầu hoàn tiền.',
  REFUND_IDEMPOTENCY_KEY_REQUIRED: 'Yêu cầu hoàn tiền đang thiếu mã chống xử lý trùng.',
  REFUND_IDEMPOTENCY_CONFLICT: 'Mã chống xử lý trùng đã được dùng cho một yêu cầu hoàn tiền khác.',
  REFUND_AMOUNT_REQUIRED: 'Vui lòng nhập số tiền cần hoàn.',
  REFUND_AMOUNT_EXCEEDS_AVAILABLE: 'Số tiền hoàn vượt quá số tiền còn có thể hoàn.',
  REFUND_COMPONENT_INVALID: 'Phạm vi hoàn tiền không phù hợp với hình thức đã chọn.',
  TICKET_REFUND_NOT_SUPPORTED: 'Hiện chưa hỗ trợ hoàn riêng từng vé. Vui lòng xử lý ở cấp toàn bộ đơn.',
  REFUND_NOT_RETRYABLE: 'Yêu cầu hoàn tiền này không thể thử lại ở trạng thái hiện tại.',
  CASH_REFUND_REQUIRES_MANUAL_SETTLEMENT: 'Hoàn tiền mặt cần được trả và xác nhận tại quầy.',
  CASH_REFUND_REFERENCE_REQUIRED: 'Vui lòng nhập mã biên nhận hoàn tiền tại quầy.',
  CASH_REFUND_NOTE_REQUIRED: 'Vui lòng ghi chú cách thức đã trả tiền cho khách.',
  MOCK_RESULT_INVALID: 'Kết quả thanh toán mô phỏng không hợp lệ.',
  VALIDATION_ERROR: 'Thông tin gửi lên chưa hợp lệ. Vui lòng kiểm tra lại.',
  INTERNAL_SERVER_ERROR: 'Hệ thống thanh toán đang bận. Vui lòng thử lại sau.',
};

export const paymentErrorCode = error =>
  error?.errorCode || error?.code || error?.response?.data?.errorCode;

export const paymentErrorMessage = error => {
  const code = paymentErrorCode(error);
  return ERROR_MESSAGES[code]
    || 'Không thể xử lý thanh toán lúc này. Vui lòng thử lại sau.';
};

export const getPaymentStatus = async paymentPublicId =>
  unwrap(await apiClient.get(`/api/payments/${paymentPublicId}/status`));

export const getPayment = async paymentPublicId =>
  unwrap(await apiClient.get(`/api/payments/${paymentPublicId}`));

export const completeMockPayment = async (paymentPublicId, simulatedStatus) =>
  unwrap(await apiClient.post(
    `/api/payments/mock/${paymentPublicId}/complete`,
    { simulatedStatus },
  ));

export const lookupCashBooking = async reference =>
  unwrap(await apiClient.get('/api/employee/payments/booking', { params: { reference } }));

export const createCashPayment = async ({ bookingPublicId, bookingCode }, idempotencyKey) =>
  unwrap(await apiClient.post(
    '/api/employee/payments/cash',
    { bookingPublicId: bookingPublicId || null, bookingCode: bookingCode || null },
    { headers: { 'Idempotency-Key': idempotencyKey } },
  ));

export const collectCashPayment = async (paymentPublicId, receivedAmount, idempotencyKey) =>
  unwrap(await apiClient.post(
    `/api/employee/payments/${paymentPublicId}/cash/collect`,
    { receivedAmount },
    { headers: { 'Idempotency-Key': idempotencyKey } },
  ));

export const cancelCashPayment = async (paymentPublicId, idempotencyKey) =>
  unwrap(await apiClient.post(
    `/api/employee/payments/${paymentPublicId}/cash/cancel`,
    {},
    { headers: { 'Idempotency-Key': idempotencyKey } },
  ));

export const searchAdminPayments = async params =>
  unwrap(await apiClient.get('/api/admin/payments', { params }));

export const getAdminPayment = async paymentPublicId =>
  unwrap(await apiClient.get(`/api/admin/payments/${paymentPublicId}`));

export const getPaymentOperations = async (kind, params) =>
  unwrap(await apiClient.get(`/api/admin/payments/${kind}`, { params }));

export const getAdminRefunds = async params =>
  unwrap(await apiClient.get('/api/admin/payments/refunds', { params }));

export const createAdminRefund = async (paymentPublicId, payload, idempotencyKey) =>
  unwrap(await apiClient.post(
    `/api/admin/payments/${paymentPublicId}/refunds`,
    payload,
    { headers: { 'Idempotency-Key': idempotencyKey } },
  ));

export const retryAdminRefund = async refundPublicId =>
  unwrap(await apiClient.post(`/api/admin/payments/refunds/${refundPublicId}/retry`));

export const completeCashRefund = async (refundPublicId, payload) =>
  unwrap(await apiClient.post(
    `/api/admin/payments/refunds/${refundPublicId}/cash/complete`,
    payload,
  ));

export const replayPaymentOperation = async (kind, id) =>
  unwrap(await apiClient.post(`/api/admin/payments/${kind}/${id}/replay`));

export const assignReconciliation = async (publicId, assigneeAccountId) =>
  unwrap(await apiClient.post(
    `/api/admin/payments/reconciliations/${publicId}/assign`,
    { assigneeAccountId },
  ));

export const resolveReconciliation = async (publicId, payload) =>
  unwrap(await apiClient.post(
    `/api/admin/payments/reconciliations/${publicId}/resolve`,
    payload,
  ));

export const exportAdminPayments = async params => {
  const response = await apiClient.get('/api/admin/payments/export', {
    params,
    responseType: 'blob',
  });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'giao-dich-thanh-toan.csv';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
};
