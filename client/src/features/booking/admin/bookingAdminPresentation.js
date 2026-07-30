export const bookingOperationalConclusion = (booking = {}, operationalInfo = {}) => {
  const attention = operationalInfo.attentionCode || booking.attentionCode;
  const paymentAttempted = Boolean(
    operationalInfo.paymentAttempted ?? booking.paymentAttempted,
  );

  if (attention === 'OVERDUE') {
    return {
      tone: 'danger',
      title: 'Đơn đã quá hạn nhưng chưa được đóng đúng trạng thái',
      detail: 'Kiểm tra lịch sử trạng thái và tác vụ hết hạn. Không xác nhận thủ công nếu chưa có kết quả Payment hợp lệ.',
      paymentLink: paymentAttempted,
    };
  }
  if (attention === 'PAYMENT_FAILED') {
    return {
      tone: 'warning',
      title: 'Đơn có lần thanh toán không thành công',
      detail: 'Ghế chỉ tiếp tục được giữ đến hạn ban đầu. Mở giao dịch liên quan để xem kết quả; không tự xác nhận đã thanh toán.',
      paymentLink: true,
    };
  }
  if (attention === 'EXPIRING_SOON') {
    return {
      tone: 'warning',
      title: 'Đơn sắp hết thời gian giữ ghế',
      detail: 'Hệ thống sẽ tự hết hạn và trả ghế nếu chưa nhận kết quả thanh toán thành công trước thời hạn.',
      paymentLink: paymentAttempted,
    };
  }

  switch (booking.bookingStatus) {
    case 'PENDING_PAYMENT':
      return paymentAttempted
        ? {
            tone: 'info',
            title: 'Đang giữ ghế và chờ kết quả thanh toán',
            detail: 'Không hủy hoặc xác nhận thủ công khi giao dịch còn đang xử lý. Có thể mở Payment để theo dõi kết quả.',
            paymentLink: true,
          }
        : {
            tone: 'info',
            title: 'Đang giữ ghế, khách chưa bắt đầu thanh toán',
            detail: 'Khách có thể tiếp tục thanh toán hoặc hủy đơn trước hạn. Hết hạn, hệ thống tự trả ghế.',
          };
    case 'CONFIRMED':
      return {
        tone: 'success',
        title: 'Đã thanh toán và ghế đã được đặt',
        detail: 'Vé đã được phát hành. Chỉ đánh dấu hoàn thành sau khi quy trình phục vụ tại rạp kết thúc.',
        paymentLink: true,
      };
    case 'COMPLETED':
      return {
        tone: 'success',
        title: 'Đơn đã hoàn thành',
        detail: 'Thanh toán và quyền sử dụng ghế đã được ghi nhận. Không có thao tác vận hành bắt buộc.',
        paymentLink: paymentAttempted,
      };
    case 'CANCELLED':
      return {
        tone: 'neutral',
        title: 'Đơn đã hủy và ghế đã được trả',
        detail: 'Đơn không thể khôi phục. Nếu khách vẫn muốn xem phim, cần tạo đơn mới.',
        paymentLink: paymentAttempted,
      };
    case 'EXPIRED':
      return {
        tone: 'neutral',
        title: 'Đơn hết hạn và ghế đã được trả',
        detail: 'Không xác nhận lại đơn cũ. Khách cần chọn ghế và tạo đơn mới.',
        paymentLink: paymentAttempted,
      };
    case 'REFUNDED':
      return {
        tone: 'warning',
        title: 'Đơn đã ghi nhận hoàn tiền',
        detail: 'Kiểm tra giao dịch Payment để theo dõi bằng chứng hoàn tiền. Ghế đã đặt không được tự động mở bán lại.',
        paymentLink: true,
      };
    default:
      return {
        tone: 'neutral',
        title: 'Đơn không cần thao tác ngay',
        detail: 'Theo dõi lịch sử trạng thái nếu cần kiểm tra thêm.',
        paymentLink: paymentAttempted,
      };
  }
};
