export const PAYMENT_STATUS_LABELS = {
  PENDING: 'Chờ khởi tạo',
  PROCESSING: 'Đang chờ kết quả',
  SUCCESS: 'Thanh toán thành công',
  FAILED: 'Thanh toán thất bại',
  CANCELLED: 'Khách đã hủy',
  EXPIRED: 'Đã hết hạn',
};

export const RECONCILIATION_STATUS_LABELS = {
  NONE: 'Không cần đối soát',
  REQUIRED: 'Cần kiểm tra',
  IN_REVIEW: 'Đang được kiểm tra',
  RESOLVED: 'Đã xử lý',
  OPEN: 'Chưa tiếp nhận',
  IGNORED: 'Đã đóng, không xử lý thêm',
};

export const DELIVERY_STATUS_LABELS = {
  PENDING: 'Chưa gửi kết quả sang hệ thống đặt vé',
  PROCESSING: 'Đang gửi kết quả sang hệ thống đặt vé',
  DELIVERED: 'Hệ thống đặt vé đã nhận kết quả',
  PUBLISHED: 'Đã gửi thành công',
  FAILED: 'Gửi kết quả chưa thành công',
  DEAD_LETTER: 'Cần nhân viên kiểm tra',
  NOT_REQUIRED: 'Không cần gửi kết quả',
};

export const OPERATION_STATUS_LABELS = {
  ...PAYMENT_STATUS_LABELS,
  ...RECONCILIATION_STATUS_LABELS,
  PUBLISHED: 'Đã gửi',
  PROCESSED: 'Đã xử lý',
  DEAD_LETTER: 'Cần nhân viên kiểm tra',
};

export const PROVIDER_LABELS = {
  VNPAY: 'VNPay',
  MOMO: 'MoMo',
  CASH: 'Tiền mặt tại quầy',
  MOCK: 'Mô phỏng nội bộ',
};

export const PAYMENT_METHOD_LABELS = {
  ONLINE: 'Thanh toán trực tuyến',
  CASH: 'Tiền mặt tại quầy',
};

export const EVENT_LABELS = {
  PAYMENT_CREATED: 'Khởi tạo giao dịch',
  PROVIDER_SESSION_CREATED: 'Mở phiên thanh toán',
  PROVIDER_CALLBACK_RECEIVED: 'Nhận kết quả từ cổng thanh toán',
  PAYMENT_PROCESSING: 'Đang xác minh kết quả',
  PAYMENT_SUCCEEDED: 'Thanh toán thành công',
  PAYMENT_FAILED: 'Thanh toán thất bại',
  PAYMENT_CANCELLED: 'Khách đã hủy thanh toán',
  PAYMENT_EXPIRED: 'Giao dịch hết hạn',
  BOOKING_RESULT_DELIVERED: 'Hệ thống đặt vé đã nhận kết quả',
  RECONCILIATION_REQUIRED: 'Chuyển sang kiểm tra đối soát',
  LATE_SUCCESS_DETECTED: 'Ghi nhận thanh toán thành công sau hạn',
  PAYMENT_RESULT: 'Kết quả thanh toán gửi sang đơn đặt vé',
  PAYMENT_SUCCEEDED_EVENT: 'Sự kiện doanh thu gửi sang báo cáo',
  REFUND_RESULT: 'Kết quả hoàn tiền gửi sang đơn đặt vé',
  REFUND_SUCCEEDED: 'Hoàn tiền thành công',
  REFUND_FAILED: 'Hoàn tiền chưa thành công',
  IPN: 'Thông báo thanh toán từ nhà cung cấp',
  QUERY: 'Kết quả truy vấn nhà cung cấp',
};

export const DESTINATION_LABELS = {
  BOOKING_SERVICE_REST: 'Hệ thống đặt vé',
  ANALYTICS_KAFKA: 'Hệ thống báo cáo doanh thu',
};

export const REASON_LABELS = {
  DELIVERY_FAILED: 'Chưa chuyển được kết quả đến hệ thống đích',
  PROVIDER_QUERY_FAILED: 'Chưa truy vấn được trạng thái từ nhà cung cấp',
  INVALID_PROVIDER_SIGNATURE: 'Chữ ký thông báo từ nhà cung cấp không hợp lệ',
  PROVIDER_AMOUNT_MISMATCH: 'Số tiền nhà cung cấp trả về không khớp với đơn',
  LATE_PROVIDER_SUCCESS: 'Nhà cung cấp xác nhận thành công sau hạn giữ ghế',
  LATE_PAYMENT_SUCCESS: 'Thanh toán thành công sau hạn giữ ghế',
  PROVIDER_EVENT_PAYLOAD_CONFLICT: 'Nhà cung cấp gửi các kết quả mâu thuẫn',
  BOOKING_RESULT_REJECTED: 'Hệ thống đặt vé không chấp nhận kết quả thanh toán',
  BOOKING_DELIVERY_REJECTED: 'Hệ thống đặt vé từ chối cập nhật kết quả',
  SHOWTIME_CANCELLED: 'Suất chiếu đã bị hủy',
  BOOKING_CONFIRMATION_FAILED: 'Đã thu tiền nhưng đơn đặt vé không thể xác nhận',
  DUPLICATE_CAPTURE: 'Nhà cung cấp ghi nhận thu tiền trùng',
  CUSTOMER_SERVICE_APPROVED: 'Hoàn toàn bộ theo quyết định chăm sóc khách hàng',
  CONCESSION_ISSUE: 'Hoàn phần bắp nước theo xác minh dịch vụ',
  PRICE_CORRECTION: 'Hoàn phần chênh lệch giá',
  OPERATIONAL_ADJUSTMENT: 'Điều chỉnh nghiệp vụ đã được phê duyệt',
};

export const RESOLUTION_OPTIONS = [
  {
    value: 'PROVIDER_CONFIRMED',
    label: 'Nhà cung cấp xác nhận đã thu tiền',
    help: 'Dùng khi đã kiểm tra trên cổng thanh toán và có bằng chứng giao dịch thành công.',
  },
  {
    value: 'PROVIDER_NOT_PAID',
    label: 'Nhà cung cấp xác nhận chưa thu tiền',
    help: 'Dùng khi cổng thanh toán xác nhận giao dịch không thành công hoặc đã hủy.',
  },
  {
    value: 'BOOKING_STATE_CONFIRMED',
    label: 'Đơn đặt vé đã ở trạng thái đúng',
    help: 'Dùng khi kết quả Payment và trạng thái đơn đã khớp, không cần cập nhật thêm.',
  },
  {
    value: 'REFUND_FOLLOW_UP_REQUIRED',
    label: 'Cần theo dõi hoàn tiền thủ công',
    help: 'Dùng khi đã thu tiền nhưng không thể phát vé và cần chuyển quy trình hoàn tiền.',
  },
  {
    value: 'NO_ACTION_REQUIRED',
    label: 'Không cần hành động thêm',
    help: 'Dùng khi đây là cảnh báo trùng hoặc sự cố đã tự phục hồi.',
  },
];

export const statusLabel = status =>
  OPERATION_STATUS_LABELS[status] || status || 'Chưa ghi nhận';

export const providerLabel = provider =>
  PROVIDER_LABELS[provider] || provider || 'Chưa ghi nhận';

export const eventLabel = event =>
  EVENT_LABELS[event] || event || 'Cập nhật hệ thống';

export const destinationLabel = destination =>
  DESTINATION_LABELS[destination] || destination || 'Hệ thống nội bộ';

export const reasonLabel = reason =>
  REASON_LABELS[reason] || reason || 'Cần kiểm tra thêm';

export const humanizeSystemMessage = message => {
  if (!message) return 'Không có ghi chú bổ sung.';
  const knownMessages = {
    'Payment attempt created from authoritative Booking context':
      'Giao dịch được tạo từ số tiền đã chốt của đơn đặt vé.',
    'Provider session created':
      'Đã mở phiên thanh toán tại nhà cung cấp.',
    'Booking Service did not accept the Payment result':
      'Hệ thống đặt vé chưa chấp nhận kết quả thanh toán.',
    'Provider reported success after the original Booking deadline':
      'Nhà cung cấp báo thành công sau khi thời gian giữ ghế đã kết thúc.',
  };
  if (knownMessages[message]) return knownMessages[message];
  if (/[À-ỹ]/u.test(message)) return message;
  return 'Hệ thống đã ghi nhận một cập nhật kỹ thuật. Mở phần dữ liệu kỹ thuật nếu cần kiểm tra chi tiết.';
};

export const paymentConclusion = payment => {
  const reconciliation = payment?.reconciliationStatus;
  const delivery = payment?.bookingDeliveryStatus;
  if (reconciliation === 'REQUIRED' || reconciliation === 'IN_REVIEW') {
    return {
      tone: 'warning',
      title: 'Giao dịch cần nhân viên kiểm tra',
      detail: reconciliation === 'IN_REVIEW'
        ? 'Hồ sơ đã có người tiếp nhận. Chỉ kết luận sau khi đối chiếu với nhà cung cấp và đơn đặt vé.'
        : 'Kết quả thanh toán và trạng thái đơn chưa đồng nhất. Hãy mở mục “Cần xử lý” để tiếp nhận hồ sơ.',
      action: 'Mở danh sách cần xử lý',
    };
  }
  if (payment?.status === 'SUCCESS' && ['DELIVERED', 'PUBLISHED'].includes(delivery)) {
    return {
      tone: 'success',
      title: 'Đã thu tiền và đơn đặt vé đã nhận kết quả',
      detail: 'Không cần thao tác thêm. Vé được quản lý tại màn hình Đơn đặt vé & giữ ghế.',
    };
  }
  if (payment?.status === 'SUCCESS') {
    return {
      tone: 'warning',
      title: 'Đã thu tiền, đang cập nhật đơn đặt vé',
      detail: 'Không thu lại tiền. Hệ thống đang chuyển kết quả sang Booking; nếu kéo dài, kiểm tra hàng đợi hệ thống.',
    };
  }
  if (payment?.status === 'PROCESSING' || payment?.status === 'PENDING') {
    return {
      tone: 'info',
      title: 'Đang chờ kết quả chính thức từ nhà cung cấp',
      detail: 'Không tạo hoặc thu thêm giao dịch cho cùng đơn khi phiên này còn hoạt động.',
    };
  }
  if (payment?.status === 'FAILED') {
    return {
      tone: 'danger',
      title: 'Giao dịch không thành công',
      detail: 'Chưa ghi nhận thu tiền. Khách có thể thử lại nếu đơn vẫn còn thời gian thanh toán.',
    };
  }
  if (payment?.status === 'CANCELLED') {
    return {
      tone: 'neutral',
      title: 'Khách đã hủy tại cổng thanh toán',
      detail: 'Chưa ghi nhận thu tiền. Đơn đặt vé vẫn tuân theo thời hạn giữ ghế ban đầu.',
    };
  }
  return {
    tone: 'neutral',
    title: 'Giao dịch đã kết thúc',
    detail: payment?.status === 'EXPIRED'
      ? 'Phiên thanh toán đã hết hạn và không còn hoạt động.'
      : 'Không có thao tác vận hành bắt buộc.',
  };
};

export const paymentActionLabel = payment => {
  if (['REQUIRED', 'IN_REVIEW'].includes(payment?.reconciliationStatus)) {
    return 'Kiểm tra đối soát';
  }
  if (payment?.status === 'PROCESSING' || payment?.status === 'PENDING') {
    return 'Theo dõi, không thu lại';
  }
  if (payment?.status === 'SUCCESS'
      && !['DELIVERED', 'PUBLISHED'].includes(payment?.bookingDeliveryStatus)) {
    return 'Chờ cập nhật đơn';
  }
  return 'Không cần thao tác';
};
