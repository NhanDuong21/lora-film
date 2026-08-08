import { useState } from 'react';
import { useParams, useOutletContext, useNavigate } from 'react-router-dom';
import { ArrowLeft, Loader2, Calendar, MapPin, Clock, Film, RefreshCw, AlertCircle, Edit, DollarSign, History } from 'lucide-react';
import useShowtimeDetail from '@/features/scheduling/admin/hooks/useShowtimeDetail';
import {
  formatShowtimeCinemaDate,
  formatShowtimeCinemaTime,
  resolveShowtimeCinemaTimezone,
} from '@/features/scheduling/admin/utils/showtimeCinemaDateTime';
import {
  getHistoryActorLabel,
  getLocalizedHistoryReason,
  getOperationalShowtimeStatus,
  getShowtimeSourcePresentation,
  getShowtimeStatusPresentation,
  getShowtimeTransitionActionPresentation,
  isExpiredDraftShowtime,
} from '@/features/scheduling/admin/utils/schedulingPresentation';

const transitionConsequences = {
  OPEN_FOR_BOOKING: 'Sau khi xác nhận, khách có thể đặt vé cho suất chiếu này ngay.',
  CLOSED: 'Sau khi xác nhận, khách sẽ không thể tạo đơn đặt vé mới cho suất chiếu này.',
  CANCELLED: 'Suất chiếu sẽ được đánh dấu đã hủy. Hãy kiểm tra các đơn đã đặt trước khi tiếp tục.',
  FINISHED: 'Suất chiếu sẽ được đánh dấu đã chiếu xong và không thể mở bán lại.',
};

const formatCurrency = (amount, currency = 'VND') => {
  if (amount == null) return 'N/A';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(amount);
};

const getStatusColor = (status) => {
  switch (status) {
    case 'DRAFT': return 'bg-zinc-500/10 text-zinc-400 border-zinc-500/20';
    case 'EXPIRED_DRAFT': return 'bg-red-500/10 text-red-300 border-red-500/25';
    case 'OPEN_FOR_BOOKING': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
    case 'CLOSED': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    case 'CANCELLED': return 'bg-red-500/10 text-red-400 border-red-500/20';
    case 'FINISHED': return 'bg-green-500/10 text-green-400 border-green-500/20';
    default: return 'bg-zinc-800 text-zinc-400 border-zinc-700';
  }
};

const getAvailableTransitions = (status) => {
  switch (status) {
    case 'DRAFT': return ['OPEN_FOR_BOOKING', 'CANCELLED'];
    case 'OPEN_FOR_BOOKING': return ['CLOSED', 'CANCELLED'];
    case 'CLOSED': return ['FINISHED', 'CANCELLED'];
    default: return [];
  }
};

const AdminShowtimeDetailPage = () => {
  const { id } = useParams();
  const { triggerToast } = useOutletContext() || {};
  const navigate = useNavigate();

  const { showtime, history, prices, isLoading, isUpdatingStatus, handleUpdateStatus, fetchDetail } = useShowtimeDetail(id, { triggerToast });

  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState('');
  const [statusReason, setStatusReason] = useState('');

  if (isLoading) {
    return (
      <div className="flex flex-col flex-1 items-center justify-center p-8 bg-zinc-950 text-zinc-400">
        <Loader2 className="w-8 h-8 animate-spin text-brand-orange mb-4" />
        <p>Đang tải chi tiết suất chiếu...</p>
      </div>
    );
  }

  if (!showtime) {
    return (
      <div className="flex flex-col flex-1 items-center justify-center p-8 bg-zinc-950 text-zinc-400">
        <AlertCircle className="w-12 h-12 text-red-500 mb-4 opacity-80" />
        <h2 className="text-xl font-bold text-white mb-2">Không tìm thấy</h2>
        <p>Suất chiếu không tồn tại.</p>
        <button onClick={() => navigate(-1)} className="mt-6 text-brand-orange hover:underline text-sm">Quay lại</button>
      </div>
    );
  }

  const expiredDraft = isExpiredDraftShowtime(showtime);
  const operationalStatus = getOperationalShowtimeStatus(showtime);
  const transitions = getAvailableTransitions(showtime.status)
    .filter(target => !(expiredDraft && target === 'OPEN_FOR_BOOKING'));
  const cinemaTimezone = showtime.cinema?.timezone;
  const timezoneResolution = resolveShowtimeCinemaTimezone(cinemaTimezone);

  const confirmTransition = async () => {
    await handleUpdateStatus(targetStatus, statusReason);
    setStatusModalOpen(false);
    setStatusReason('');
  };

  return (
    <div className="flex flex-col flex-1 bg-zinc-950 text-white min-h-[400px] animate-fade-in relative">
      
      {/* Header */}
      <div className="sticky top-0 z-20 bg-zinc-950/80 backdrop-blur-md border-b border-zinc-800 p-6 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-zinc-800 rounded-xl transition-colors text-zinc-400 hover:text-white flex-shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-xl md:text-2xl font-black tracking-tight text-white">
                Chi tiết suất chiếu
              </h1>
              <span
                aria-label={`Trạng thái hiện tại: ${getShowtimeStatusPresentation(operationalStatus).label}`}
                className={`px-2.5 py-1 text-xs font-bold rounded-full border ${getStatusColor(operationalStatus)}`}
              >
                {getShowtimeStatusPresentation(operationalStatus).label}
              </span>
            </div>
            <p className="text-zinc-500 text-sm mt-1">Kiểm tra thông tin phim, phòng và giá trước khi mở bán.</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchDetail}
            className="p-2.5 rounded-xl border border-zinc-800 hover:bg-zinc-800 text-zinc-400 transition-colors"
            title="Làm mới"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          {transitions.length > 0 && (
            <div className="flex gap-2">
              {transitions.map(t => (
                <button
                  key={t}
                  onClick={() => {
                    setTargetStatus(t);
                    setStatusModalOpen(true);
                  }}
                  disabled={isUpdatingStatus || (t === 'OPEN_FOR_BOOKING' && prices?.complete === false)}
                  title={isUpdatingStatus
                    ? 'Đang cập nhật trạng thái suất chiếu; vui lòng đợi.'
                    : (t === 'OPEN_FOR_BOOKING' && prices?.complete === false
                      ? 'Không thể mở bán vì chưa đủ giá cho tất cả loại ghế.'
                      : undefined)}
                  className="bg-zinc-900 border border-zinc-700 hover:bg-zinc-800 text-zinc-200 font-bold px-4 py-2 rounded-xl text-xs uppercase tracking-wider transition-colors"
                >
                  {getShowtimeTransitionActionPresentation(t).label}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {expiredDraft && (
        <div className="mx-6 mt-5 flex items-start gap-3 rounded-2xl border border-red-500/25 bg-red-500/10 p-4 text-red-100 md:mx-8" role="alert">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" aria-hidden="true" />
          <div>
            <p className="font-black">Suất chiếu đã qua giờ bắt đầu</p>
            <p className="mt-1 text-sm text-red-100/70">Hệ thống không cho phép mở bán suất này. Dữ liệu vẫn được giữ để đối soát; bạn có thể hủy suất để hoàn tất xử lý.</p>
          </div>
        </div>
      )}

      <div className="p-6 md:p-8 space-y-6 max-w-[1600px] mx-auto w-full grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Column: Info & Pricing */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Main Info Card */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6">
              <h2 className="text-base font-black text-zinc-200 flex items-center gap-2 border-b border-zinc-800 pb-3 mb-4">
              <Film className="w-4 h-4 text-brand-orange" />
              Thông tin lịch chiếu
            </h2>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <span className="text-xs text-zinc-500 font-bold">Phim</span>
                  <p className="font-bold text-white text-lg mt-1">{showtime.movie?.title}</p>
                </div>
                <div>
                  <span className="text-xs text-zinc-500 font-bold">Định dạng</span>
                  <div className="flex flex-wrap gap-2 mt-1.5">
                    <span className="bg-zinc-800 px-2 py-0.5 rounded text-xs">{showtime.movieVersion?.versionName}</span>
                    <span className="bg-zinc-800 px-2 py-0.5 rounded text-xs">{showtime.movieVersion?.format}</span>
                    <span className="bg-zinc-800 px-2 py-0.5 rounded text-xs">{showtime.movieVersion?.audioLanguage}</span>
                  </div>
                </div>
              </div>

              <div className="space-y-4">
                <div>
                  <span className="text-xs text-zinc-500 font-bold">Rạp và phòng</span>
                  <p className="text-white mt-1 flex items-center gap-2">
                    <MapPin className="w-4 h-4 text-zinc-400" />
                    <span>{showtime.cinema?.name} - Phòng: <strong>{showtime.auditorium?.name}</strong></span>
                  </p>
                  <p className="mt-1 text-xs text-zinc-500">
                    Giờ địa phương: {timezoneResolution.usedFallback ? 'Mặc định' : 'Theo rạp'}
                    <span className="sr-only"> Múi giờ: {timezoneResolution.timezone}</span>
                  </p>
                </div>
                <div>
                  <span className="text-xs text-zinc-500 font-bold">Thời gian chiếu</span>
                  <p className="text-white mt-1 flex items-center gap-2 text-lg">
                    <Clock className="w-4 h-4 text-zinc-400" />
                    <span className="font-bold text-brand-orange">{formatShowtimeCinemaTime(showtime.startTime, cinemaTimezone)}</span>
                    <span className="text-zinc-500 text-sm">đến</span>
                    <span className="font-bold">{formatShowtimeCinemaTime(showtime.endTime, cinemaTimezone)}</span>
                  </p>
                  <p className="text-sm text-zinc-400 mt-1 flex items-center gap-2">
                    <Calendar className="w-3 h-3" /> Ngày {formatShowtimeCinemaDate(showtime.startTime, cinemaTimezone)}
                  </p>
                  {timezoneResolution.usedFallback && (
                    <p className="mt-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-2 py-1 text-xs text-amber-300" role="status">
                      Cấu hình giờ của rạp đang bị lỗi; thời gian tạm hiển thị theo giờ chuẩn hệ thống.
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Pricing Info */}
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6">
            <div className="mb-4 flex items-center justify-between border-b border-zinc-800 pb-3">
              <h2 className="text-base font-black text-zinc-200 flex items-center gap-2">
                <DollarSign className="w-4 h-4 text-brand-orange" />
                Giá vé của suất chiếu
              </h2>
              <button
                type="button"
                onClick={() => navigate(`/admin/showtimes/${id}/pricing`)}
                className="text-xs font-bold text-brand-orange hover:underline"
              >
                Kiểm tra giá
              </button>
            </div>

            {prices?.complete === false && (
              <div className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200">
                Giá vé chưa đầy đủ. Chưa thể mở bán; còn thiếu {prices.missingSeatTypes?.length || 0} loại ghế
                {prices.ambiguousSeatTypes?.length ? ` và ${prices.ambiguousSeatTypes.length} loại ghế bị mơ hồ` : ''}.
                {showtime.status === 'DRAFT' && (
                  <button type="button" onClick={() => navigate(`/admin/showtimes/${id}/pricing`)} className="ml-2 font-black underline">
                    Bổ sung giá
                  </button>
                )}
              </div>
            )}

            {!prices || prices.prices?.length === 0 ? (
              <p className="text-zinc-500 text-sm italic">Không có dữ liệu giá. Giá chưa được cấu hình cho suất chiếu này.</p>
            ) : (
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                {prices.prices.map(p => (
                  <div key={p.seatTypeId} className="bg-zinc-950 border border-zinc-800 p-4 rounded-xl">
                    <span className="text-sm text-zinc-200 font-bold mb-1 block">{p.seatTypeName || 'Loại ghế chưa đặt tên'}</span>
                    <span className="text-[10px] text-zinc-500 font-bold uppercase tracking-wider mb-2 block">{p.seatTypeCode || 'Không có mã'}</span>
                    <span className="text-lg font-black text-green-400">{formatCurrency(p.price, prices.currency)}</span>
                    <p className="mt-2 text-[10px] text-zinc-500">{p.pricingSource ? 'Theo bảng giá của rạp' : 'Giá nhập trực tiếp'}</p>
                    <details className="mt-2 text-[10px] text-zinc-500">
                      <summary className="cursor-pointer font-bold">Thông tin kỹ thuật</summary>
                      <p className="mt-1 break-all font-mono">seatTypeId: {p.seatTypeId}</p>
                    </details>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: History */}
        <div className="lg:col-span-1 space-y-8">
          <div className="bg-zinc-900/60 border border-zinc-800 rounded-2xl p-6 h-full">
              <h2 className="text-base font-black text-zinc-200 flex items-center gap-2 border-b border-zinc-800 pb-3 mb-6">
                <History className="w-4 h-4 text-brand-orange" />
              Lịch sử thay đổi
            </h2>

            <div className="space-y-6 relative before:absolute before:inset-0 before:ml-2.5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-zinc-800 before:to-transparent">
              {history.length === 0 ? (
                <p className="text-zinc-500 text-xs text-center relative z-10 bg-zinc-900 py-2">Chưa có lịch sử</p>
              ) : (
                history.map((h, i) => (
                  <div key={i} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group is-active">
                    
                    {/* Icon */}
                    <div className={`flex items-center justify-center w-6 h-6 rounded-full border-2 border-zinc-900 bg-zinc-800 text-zinc-400 group-[.is-active]:text-white ${h.newStatus === 'CANCELLED' ? 'group-[.is-active]:bg-red-500' : 'group-[.is-active]:bg-brand-orange'} shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow absolute left-0 md:left-1/2 transform -translate-x-1/2`}>
                      <span className="w-2 h-2 rounded-full bg-current"></span>
                    </div>
                    
                    {/* Card */}
                    <div className="w-[calc(100%-2.5rem)] md:w-[calc(50%-1.5rem)] bg-zinc-950 p-4 rounded-xl border border-zinc-800/80 shadow">
                      <div className="flex items-center justify-between mb-1">
                        <span className={`px-1.5 py-0.5 rounded text-[9px] font-black tracking-wider uppercase border ${getStatusColor(h.newStatus)}`}>
                          {h.previousStatus ? getShowtimeStatusPresentation(h.previousStatus).label : 'Khởi tạo'} → {getShowtimeStatusPresentation(h.newStatus).label}
                        </span>
                        <time className="text-[10px] text-zinc-500 font-medium">
                          {formatShowtimeCinemaTime(h.changedAt, cinemaTimezone)} - {formatShowtimeCinemaDate(h.changedAt, cinemaTimezone)}
                        </time>
                      </div>
                      <p className="text-xs text-zinc-400 mt-2 bg-zinc-900/50 p-2 rounded">{getLocalizedHistoryReason(h.reason)}</p>
                      <p className="mt-2 text-[10px] text-zinc-500">
                        {getHistoryActorLabel(h.changedBy)} · {getShowtimeSourcePresentation(h.source).label}
                      </p>
                      {h.previewPublicId && (
                        <button
                          type="button"
                          onClick={() => navigate(`/admin/showtime-schedules/${encodeURIComponent(h.previewPublicId)}`)}
                          className="mt-2 text-[10px] font-bold text-brand-orange hover:underline"
                        >
                          Mở lịch đã tạo suất này
                        </button>
                      )}
                      <details className="mt-2 text-[10px] text-zinc-500">
                        <summary className="cursor-pointer font-bold">Thông tin kỹ thuật</summary>
                        <div className="mt-1 break-all font-mono">
                          <p>{h.previousStatus || 'null'} → {h.newStatus || 'null'}</p>
                          <p>changedBy: {h.changedBy ?? 'null'}</p>
                          <p>changedAt: {h.changedAt || 'null'}</p>
                          <p>source: {h.source || 'null'}</p>
                          <p>previewPublicId: {h.previewPublicId || 'null'}</p>
                          <p>reason: {h.reason || 'null'}</p>
                        </div>
                      </details>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Transition Modal */}
      {statusModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4 animate-fade-in">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-6 w-full max-w-md shadow-2xl">
            <h3 className="text-lg font-bold text-white mb-2">
              {getShowtimeTransitionActionPresentation(targetStatus).label} suất chiếu này?
            </h3>
            <p className="text-sm text-zinc-400 mb-4">
              Bạn đang đổi tình trạng suất chiếu sang <strong className={`px-1.5 py-0.5 rounded text-xs font-bold border ${getStatusColor(targetStatus)}`}>{getShowtimeStatusPresentation(targetStatus).label}</strong>.
            </p>
            <p className="mb-4 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-sm leading-6 text-amber-100">
              {transitionConsequences[targetStatus] || 'Thay đổi này sẽ được ghi vào lịch sử suất chiếu.'}
            </p>
            
            <label className="block text-xs font-semibold text-zinc-400 mb-1">Lý do thay đổi (Tuỳ chọn)</label>
            <textarea
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              placeholder="Bạn có thể ghi chú lý do thay đổi (không bắt buộc)…"
              className="w-full bg-zinc-950 border border-zinc-800 text-zinc-200 focus:border-brand-orange/40 rounded-xl py-2 px-3 text-sm transition-colors focus:outline-none min-h-[80px]"
            />

            <div className="flex items-center justify-end gap-3 mt-6">
              <button 
                onClick={() => setStatusModalOpen(false)}
                className="px-4 py-2 rounded-xl text-sm font-bold text-zinc-400 hover:text-white hover:bg-zinc-800 transition-colors"
              >
                Hủy
              </button>
              <button 
                onClick={confirmTransition}
                disabled={isUpdatingStatus}
                className="bg-brand-orange text-zinc-950 px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2 hover:bg-opacity-90 disabled:opacity-50"
              >
                {isUpdatingStatus ? <Loader2 className="w-4 h-4 animate-spin" /> : <Edit className="w-4 h-4" />}
                {getShowtimeTransitionActionPresentation(targetStatus).label}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default AdminShowtimeDetailPage;
