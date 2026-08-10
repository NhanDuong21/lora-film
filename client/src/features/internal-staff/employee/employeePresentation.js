export const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: currency || 'VND',
  maximumFractionDigits: 0,
}).format(Number(value || 0));

export const dateTime = value => value
  ? new Date(value).toLocaleString('vi-VN', {
    hour: '2-digit', minute: '2-digit',
    day: '2-digit', month: '2-digit', year: 'numeric', hour12: false,
  })
  : 'Chưa ghi nhận';

export const clock = value => value
  ? new Date(value).toLocaleTimeString('vi-VN', {
    hour: '2-digit', minute: '2-digit', hour12: false,
  })
  : '--:--';

export const BOOKING_STATUS = {
  PENDING_PAYMENT: { label: 'Chờ thanh toán', tone: 'border-amber-500/30 bg-amber-500/10 text-amber-300' },
  CONFIRMED: { label: 'Đã thanh toán', tone: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
  COMPLETED: { label: 'Đã hoàn thành', tone: 'border-sky-500/30 bg-sky-500/10 text-sky-300' },
  CANCELLED: { label: 'Đã hủy', tone: 'border-red-500/30 bg-red-500/10 text-red-300' },
  EXPIRED: { label: 'Hết thời gian giữ ghế', tone: 'border-zinc-700 bg-zinc-800 text-zinc-400' },
  REFUNDED: { label: 'Đã hoàn tiền', tone: 'border-violet-500/30 bg-violet-500/10 text-violet-300' },
};

export const bookingStatus = status => BOOKING_STATUS[status] || {
  label: 'Đang cập nhật', tone: 'border-zinc-700 bg-zinc-800 text-zinc-400',
};

export const shiftStatus = status => ({
  SCHEDULED: 'Đã xếp ca',
  IN_PROGRESS: 'Đang trong ca',
  COMPLETED: 'Đã kết thúc',
  CANCELLED: 'Đã hủy',
}[status] || 'Đang cập nhật');

export const attendanceStatus = status => ({
  PRESENT: 'Có mặt',
  LATE: 'Đi muộn',
  EARLY_LEAVE: 'Về sớm',
  ABSENT: 'Vắng mặt',
  COMPLETED: 'Đã hoàn tất',
  IN_PROGRESS: 'Đang trong ca',
}[status] || shiftStatus(status));

export const leaveStatus = status => ({
  PENDING: 'Chờ quản lý duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
}[status] || 'Đang cập nhật');

export const leaveType = type => ({
  ANNUAL: 'Phép năm',
  SICK: 'Nghỉ bệnh',
  UNPAID: 'Nghỉ không lương',
  OTHER: 'Nghỉ khác',
}[type] || 'Loại nghỉ khác');

export const payrollStatus = status => ({
  DRAFT: 'Bản nháp',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  PAYMENT_PENDING: 'Chờ kế toán đối soát',
  PAID: 'Đã thanh toán',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
}[status] || 'Đang cập nhật');

const PRODUCT_NAMES = {
  'Mineral Water': 'Nước suối',
  'Popcorn Size S': 'Bắp rang cỡ nhỏ',
  'Popcorn Size L': 'Bắp rang cỡ lớn',
  'Cheese Hot Dog': 'Xúc xích phô mai',
  'Cheese Popcorn': 'Bắp rang phô mai',
  'Caramel Popcorn': 'Bắp rang caramel',
  'Coca-Cola Size L': 'Coca-Cola cỡ lớn',
  'Pepsi Size L': 'Pepsi cỡ lớn',
  'Lemon Tea': 'Trà chanh',
  'Combo Classic': 'Combo xem phim cổ điển',
  'Combo Date': 'Combo cặp đôi',
  'Combo Family': 'Combo gia đình',
  'Combo Kids': 'Combo trẻ em',
};

export const productName = value => PRODUCT_NAMES[value] || value || 'Sản phẩm tại quầy';

export const foodNames = value => String(value || '')
  .split(',')
  .map(part => {
    const item = part.trim();
    const match = item.match(/^(.*?)(\s+x\d+)$/i);
    return match ? `${productName(match[1].trim())}${match[2]}` : productName(item);
  })
  .filter(Boolean)
  .join(', ');

export const auditoriumLabel = value => (value || 'Chưa ghi nhận phòng')
  .replace(/\bScreen\b/gi, 'Phòng')
  .replace(/\bStandard\b/gi, 'Tiêu chuẩn')
  .replace(/\bPremium\b/gi, 'Cao cấp');

export const TICKET_SCAN_RESULTS = {
  ADMITTED: { label: 'Đã cho khách vào', shortLabel: 'Hợp lệ', tone: 'emerald' },
  ALREADY_USED: { label: 'Vé đã được sử dụng', shortLabel: 'Đã dùng', tone: 'red' },
  NOT_FOUND: { label: 'Không tìm thấy vé', shortLabel: 'Không tìm thấy', tone: 'red' },
  WRONG_CINEMA: { label: 'Vé không thuộc rạp này', shortLabel: 'Sai rạp', tone: 'red' },
  TOO_EARLY: { label: 'Chưa đến giờ vào phòng', shortLabel: 'Quá sớm', tone: 'amber' },
  TOO_LATE: { label: 'Suất chiếu đã kết thúc', shortLabel: 'Quá muộn', tone: 'red' },
  REFUNDED: { label: 'Vé đã hoàn tiền', shortLabel: 'Đã hoàn', tone: 'red' },
  CANCELLED: { label: 'Vé đã bị hủy', shortLabel: 'Đã hủy', tone: 'red' },
  NOT_PAID: { label: 'Đơn chưa thanh toán', shortLabel: 'Chưa thanh toán', tone: 'amber' },
  INVALID_STATUS: { label: 'Vé chưa sẵn sàng để sử dụng', shortLabel: 'Không hợp lệ', tone: 'red' },
};

export const ticketScanResult = result => TICKET_SCAN_RESULTS[result] || {
  label: 'Đang kiểm tra trạng thái vé', shortLabel: 'Chưa rõ', tone: 'zinc',
};

export const entryStatus = status => ({
  OPEN: { label: 'Đang đón khách', tone: 'emerald' },
  UPCOMING: { label: 'Sắp mở cửa', tone: 'amber' },
  CLOSED: { label: 'Đã đóng cửa', tone: 'zinc' },
}[status] || { label: 'Đang cập nhật', tone: 'zinc' });

export const seatCount = booking => booking?.snapshot?.seats?.length
  || booking?.presentation?.seats?.length
  || 0;
