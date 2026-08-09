import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  AlertTriangle,
  BarChart3,
  Building2,
  CalendarDays,
  Check,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  Film,
  Lightbulb,
  RefreshCw,
  Search,
  Target,
  Ticket,
  TrendingDown,
  TrendingUp,
  X,
} from 'lucide-react';
import { EmptyWorkspace, HrHero } from '../../admin/components/HrWorkspace';
import { ConsolePanel, MetricStrip } from '../../admin/components/OperationsConsole';
import managerCinemaService from '../services/managerCinemaService';

const DAY_IN_MS = 86_400_000;

const formatIsoDate = date => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 10);
};

const shiftIsoDate = (value, days) => {
  const date = new Date(`${value}T00:00:00`);
  date.setDate(date.getDate() + days);
  return formatIsoDate(date);
};

const rangeFor = days => {
  const end = new Date();
  const start = new Date(end);
  start.setDate(end.getDate() - days + 1);
  return { startDate: formatIsoDate(start), endDate: formatIsoDate(end) };
};

const previousRangeFor = (startDate, endDate) => {
  const start = new Date(`${startDate}T00:00:00`);
  const end = new Date(`${endDate}T00:00:00`);
  const days = Math.max(1, Math.round((end.getTime() - start.getTime()) / DAY_IN_MS) + 1);
  return {
    startDate: shiftIsoDate(startDate, -days),
    endDate: shiftIsoDate(startDate, -1),
  };
};

const DATE_RANGES = [
  { days: 1, label: 'Hôm nay' },
  { days: 7, label: '7 ngày' },
  { days: 30, label: '30 ngày' },
  { days: 90, label: '90 ngày' },
];

const DECISION_VIEWS = [
  { value: 'result', eyebrow: '01 · Kết quả', label: 'Đã xảy ra gì?', description: 'Kết quả và xu hướng tại rạp' },
  { value: 'cause', eyebrow: '02 · Nguyên nhân', label: 'Vì sao?', description: 'Tín hiệu cần quản lý chú ý' },
  { value: 'action', eyebrow: '03 · Hành động', label: 'Nên làm gì?', description: 'Việc cần nhận và xử lý' },
];

const STATUS_LABELS = {
  SUCCESS: 'Thành công',
  FAILED: 'Gặp lỗi',
  RUNNING: 'Đang tổng hợp',
  NEVER_RUN: 'Chưa từng tổng hợp',
  FRESH: 'Dữ liệu mới',
  DEGRADED: 'Cập nhật chậm',
  STALE: 'Dữ liệu đã cũ',
  NO_DATA: 'Chưa có dữ liệu',
  CRITICAL: 'Nghiêm trọng',
  HIGH: 'Mức cao',
  WARNING: 'Cần chú ý',
  MEDIUM: 'Mức vừa',
  LOW: 'Mức thấp',
  INFO: 'Thông tin',
  PENDING: 'Chờ xử lý',
  ACCEPTED: 'Đang xử lý',
  COMPLETED: 'Đã hoàn tất',
  DISMISSED: 'Đã bỏ qua',
};

const PRIORITY_LABELS = {
  URGENT: 'Khẩn cấp',
  HIGH: 'Ưu tiên cao',
  MEDIUM: 'Ưu tiên vừa',
  LOW: 'Ưu tiên thấp',
};

const SERVICE_LABELS = {
  'analytics-service': 'Bộ phận dữ liệu',
  'booking-service': 'Bộ phận đơn hàng',
  'business-operations': 'Bộ phận kinh doanh',
  'movie-service': 'Bộ phận lịch chiếu',
  'payment-service': 'Bộ phận thanh toán',
  'promotion-service': 'Bộ phận ưu đãi',
};

const CAUSE_LABELS = {
  CINEMA_REVENUE_DECLINE: 'Doanh thu tại rạp giảm',
  CINEMA_REFUND_PRESSURE: 'Hoàn tiền tập trung tại rạp',
  CINEMA_LOW_OCCUPANCY: 'Tỷ lệ lấp đầy ghế tại rạp thấp',
  MISSING_EVENT_SNAPSHOT: 'Dữ liệu đầu vào còn thiếu',
  SYSTEM_METRIC_DEVIATION: 'Kết quả thay đổi ngoài mức thông thường',
};

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
}).format(Number(value) || 0);

const number = value => new Intl.NumberFormat('vi-VN').format(Number(value) || 0);
const percent = value => `${((Number(value) || 0) * 100).toLocaleString('vi-VN', { maximumFractionDigits: 1, minimumFractionDigits: 1 })}%`;
const displayDate = value => {
  if (!value) return 'chưa xác định';
  const [year, month, day] = String(value).slice(0, 10).split('-');
  return year && month && day ? `${day}/${month}/${year}` : value;
};

const percentChange = (current, previous) => {
  const currentValue = Number(current) || 0;
  const previousValue = Number(previous) || 0;
  if (!previousValue) return null;
  return (currentValue - previousValue) / Math.abs(previousValue);
};

const comparisonText = (current, previous, neutralText = 'Kỳ trước chưa có dữ liệu') => {
  const delta = percentChange(current, previous);
  if (delta == null) return neutralText;
  const prefix = delta > 0 ? '+' : '';
  return `${prefix}${percent(delta)} so với kỳ trước`;
};

const toneForStatus = status => {
  if (['CRITICAL', 'FAILED'].includes(status)) return 'border-red-500/25 bg-red-500/10 text-red-300';
  if (['HIGH', 'WARNING', 'DEGRADED', 'STALE'].includes(status)) return 'border-amber-500/25 bg-amber-500/10 text-amber-300';
  if (['SUCCESS', 'FRESH', 'COMPLETED', 'ACCEPTED'].includes(status)) return 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300';
  return 'border-white/10 bg-white/5 text-zinc-300';
};

const localizeText = (value, cinema) => {
  if (!value) return 'Chưa có mô tả.';
  let result = String(value);
  if (cinema?.id) result = result.split(cinema.id).join(cinema.name);
  if (cinema?.publicId) result = result.split(cinema.publicId).join(cinema.name);
  const replacements = {
    'Refund Rate': 'Tỷ lệ hoàn tiền',
    Occupancy: 'Tỷ lệ lấp đầy ghế',
    'root-cause': 'nguyên nhân',
    snapshot: 'thông tin chi tiết',
    Snapshot: 'Thông tin chi tiết',
    booking: 'đơn hàng',
    Booking: 'Đơn hàng',
  };
  Object.entries(replacements).forEach(([source, replacement]) => {
    result = result.split(source).join(replacement);
  });
  return result;
};

const primaryCauseBelongsToCinema = (insight, cinema) => {
  const cinemaKey = String(cinema?.publicId || cinema?.id || '');
  const primaryCause = [...(insight?.rootCauses || [])]
    .sort((left, right) => Number(left.rank || 0) - Number(right.rank || 0))[0];
  return Boolean(cinemaKey)
    && primaryCause?.dimensionType === 'CINEMA'
    && String(primaryCause.dimensionKey) === cinemaKey;
};

function Badge({ value, label }) {
  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-bold ${toneForStatus(value)}`}>
      {label || STATUS_LABELS[value] || 'Chưa xác định'}
    </span>
  );
}

function SectionHeading({ icon: Icon, eyebrow, title, description, aside }) {
  return (
    <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
      <div>
        <p className="flex items-center gap-2 text-[10px] font-black uppercase tracking-[0.18em] text-orange-400">
          {Icon ? <Icon size={15} /> : null} {eyebrow}
        </p>
        <h2 className="mt-2 text-xl font-black text-white">{title}</h2>
        {description ? <p className="mt-1 text-sm leading-6 text-zinc-500">{description}</p> : null}
      </div>
      {aside}
    </div>
  );
}

function RevenueChart({ values = [] }) {
  if (!values.length) {
    return <EmptyWorkspace title="Chưa có doanh thu trong kỳ" description="Hãy đổi khoảng thời gian hoặc kiểm tra lại trạng thái tổng hợp dữ liệu." />;
  }

  const width = 900;
  const height = 230;
  const padding = 24;
  const maximum = Math.max(...values.map(item => Number(item.netRevenue) || 0), 1);
  const points = values.map((item, index) => {
    const x = values.length === 1 ? width / 2 : padding + index * ((width - padding * 2) / (values.length - 1));
    const y = height - padding - ((Number(item.netRevenue) || 0) / maximum) * (height - padding * 2);
    return { ...item, x, y };
  });
  const path = points.map(point => `${point.x},${point.y}`).join(' ');

  return (
    <div className="mt-6">
      <div className="overflow-hidden rounded-xl border border-white/5 bg-black/20 p-3">
        <svg viewBox={`0 0 ${width} ${height}`} className="h-56 w-full" role="img" aria-label="Biểu đồ doanh thu thuần theo ngày">
          {[0.25, 0.5, 0.75].map(level => (
            <line key={level} x1={padding} x2={width - padding} y1={height * level} y2={height * level} stroke="rgba(255,255,255,0.06)" />
          ))}
          <polyline fill="none" stroke="#f97316" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" points={path} />
          {points.map(point => <circle key={point.statDate} cx={point.x} cy={point.y} r="5" fill="#09090b" stroke="#fb923c" strokeWidth="3" />)}
        </svg>
      </div>
      <div className="mt-3 flex justify-between text-[11px] font-semibold text-zinc-600">
        <span>{displayDate(values[0]?.statDate)}</span>
        <span>Đỉnh kỳ: {money(maximum)}</span>
        <span>{displayDate(values.at(-1)?.statDate)}</span>
      </div>
    </div>
  );
}

function ChangeHint({ current, previous, reverse = false }) {
  const delta = percentChange(current, previous);
  if (delta == null) return <span className="text-zinc-600">Kỳ trước chưa có dữ liệu</span>;
  const isGood = reverse ? delta <= 0 : delta >= 0;
  const Icon = delta >= 0 ? TrendingUp : TrendingDown;
  return (
    <span className={`inline-flex items-center gap-1 font-bold ${isGood ? 'text-emerald-400' : 'text-amber-300'}`}>
      <Icon size={13} /> {comparisonText(current, previous)}
    </span>
  );
}

function ResultView({ report, previousReport, selectedCinema }) {
  const summary = report?.summary || {};
  const previous = previousReport?.summary || {};
  const daily = report?.daily || [];
  const sortedDays = [...daily].sort((left, right) => Number(right.netRevenue || 0) - Number(left.netRevenue || 0));
  const bestDay = sortedDays[0];
  const weakestDay = sortedDays.at(-1);
  const hasData = daily.length > 0 || Number(summary.bookingCount) > 0;

  return (
    <div className="space-y-5">
      <SectionHeading
        icon={BarChart3}
        eyebrow="Đã xảy ra gì?"
        title={`Hiệu suất riêng · ${selectedCinema.name}`}
        description={`${displayDate(report?.period?.startDate)} – ${displayDate(report?.period?.endDate)} · so sánh với kỳ liền trước có cùng số ngày.`}
      />

      <MetricStrip items={[
        { icon: CircleDollarSign, label: 'Doanh thu thuần', value: money(summary.netRevenue), hint: <ChangeHint current={summary.netRevenue} previous={previous.netRevenue} />, tone: 'orange' },
        { icon: Ticket, label: 'Đơn hàng thành công', value: number(summary.bookingCount), hint: <ChangeHint current={summary.bookingCount} previous={previous.bookingCount} />, tone: 'blue' },
        { icon: Target, label: 'Công suất ghế', value: percent(summary.occupancyRate), hint: <ChangeHint current={summary.occupancyRate} previous={previous.occupancyRate} />, tone: 'green' },
        { icon: AlertTriangle, label: 'Tỷ lệ hoàn tiền', value: percent(summary.refundRate), hint: <ChangeHint current={summary.refundRate} previous={previous.refundRate} reverse />, tone: 'amber' },
      ]} />

      <div className="rounded-2xl border border-orange-500/20 bg-orange-500/5 px-5 py-4 text-sm leading-6 text-zinc-400">
        {hasData ? (
          <>Mọi con số trên màn hình chỉ lấy giao dịch của <strong className="text-zinc-200">{selectedCinema.name}</strong>. Dự báo và điểm sức khỏe toàn chuỗi không được trộn vào báo cáo rạp.</>
        ) : (
          <><strong className="text-zinc-200">{selectedCinema.name}</strong> chưa phát sinh giao dịch trong khoảng thời gian này. Giá trị bằng 0 là trạng thái chưa có dữ liệu, không phải lỗi hệ thống.</>
        )}
      </div>

      <ConsolePanel className="p-5">
        <SectionHeading
          icon={TrendingUp}
          eyebrow="Xu hướng doanh thu"
          title="Doanh thu thuần theo ngày"
          description="Số tiền thực nhận sau giảm giá và hoàn tiền; dùng để nhận ra ngày tăng hoặc giảm bất thường."
          aside={<span className="inline-flex items-center gap-2 text-xs text-zinc-600"><CalendarDays size={15} /> {daily.length} ngày có dữ liệu</span>}
        />
        <RevenueChart values={daily} />
        {bestDay ? (
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border border-emerald-500/15 bg-emerald-500/5 p-4">
              <p className="text-[10px] font-black uppercase tracking-wider text-emerald-400">Ngày tốt nhất</p>
              <p className="mt-2 text-sm font-black text-zinc-200">{displayDate(bestDay.statDate)} · {money(bestDay.netRevenue)}</p>
              <p className="mt-1 text-xs text-zinc-500">{number(bestDay.ticketCount)} vé · công suất {percent(bestDay.occupancyRate)}</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/[0.02] p-4">
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-500">Ngày thấp nhất</p>
              <p className="mt-2 text-sm font-black text-zinc-200">{displayDate(weakestDay.statDate)} · {money(weakestDay.netRevenue)}</p>
              <p className="mt-1 text-xs text-zinc-500">{number(weakestDay.ticketCount)} vé · công suất {percent(weakestDay.occupancyRate)}</p>
            </div>
          </div>
        ) : null}
      </ConsolePanel>

      <section className="grid gap-5 xl:grid-cols-2">
        <ConsolePanel className="p-5">
          <SectionHeading icon={Film} eyebrow="Hiệu suất nội dung" title="Phim mang lại doanh thu cao" description="Dùng để ưu tiên khung giờ và phòng chiếu phù hợp." />
          <div className="mt-5 space-y-3">
            {(report?.topMovies || []).slice(0, 6).map((movie, index) => (
              <article key={movie.movieKey} className="grid grid-cols-[36px_1fr_auto] items-center gap-3 rounded-xl border border-white/5 bg-white/[0.02] p-3">
                <span className="grid h-9 w-9 place-items-center rounded-lg bg-orange-500/10 text-xs font-black text-orange-400">{index + 1}</span>
                <div className="min-w-0">
                  <p className="truncate text-sm font-black text-zinc-200">{movie.movieTitle}</p>
                  <p className="mt-1 text-xs text-zinc-600">{number(movie.ticketCount)} vé · lấp đầy {percent(movie.occupancyRate)}</p>
                </div>
                <p className="text-right text-xs font-black text-zinc-300">{money(movie.netRevenue)}</p>
              </article>
            ))}
            {!(report?.topMovies || []).length ? <EmptyWorkspace title="Chưa có dữ liệu phim" description="Danh sách sẽ xuất hiện khi rạp có giao dịch vé trong kỳ." /> : null}
          </div>
        </ConsolePanel>

        <ConsolePanel className="p-5">
          <SectionHeading icon={Target} eyebrow="Hiệu quả ưu đãi" title="Khuyến mãi đang tạo doanh thu" description="So sánh doanh thu tạo ra với chi phí giảm giá." />
          <div className="mt-5 space-y-3">
            {(report?.promotions || []).slice(0, 6).map((promotion, index) => (
              <article key={promotion.promotionKey} className="rounded-xl border border-white/5 bg-white/[0.02] p-4">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-black text-zinc-200">{index + 1}. {promotion.promotionName || 'Ưu đãi chưa đặt tên'}</p>
                    <p className="mt-1 text-xs text-zinc-600">{number(promotion.usageCount)} lượt dùng · chi phí {money(promotion.discountCost)}</p>
                  </div>
                  <Badge value={Number(promotion.roi) >= 1 ? 'SUCCESS' : 'WARNING'} label={`ROI ${Number(promotion.roi || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })}x`} />
                </div>
                <p className="mt-3 text-xs text-zinc-500">Doanh thu tạo ra <strong className="text-zinc-300">{money(promotion.generatedRevenue)}</strong></p>
              </article>
            ))}
            {!(report?.promotions || []).length ? <EmptyWorkspace title="Chưa có dữ liệu khuyến mãi" description="Không có lượt sử dụng ưu đãi tại rạp trong kỳ đang xem." /> : null}
          </div>
        </ConsolePanel>
      </section>
    </div>
  );
}

function CauseView({ report, previousReport, selectedCinema }) {
  const summary = report?.summary || {};
  const previous = previousReport?.summary || {};
  const derivedSignals = [];
  const revenueDelta = percentChange(summary.netRevenue, previous.netRevenue);
  if (revenueDelta != null && revenueDelta <= -0.1) derivedSignals.push({ severity: 'WARNING', title: 'Doanh thu giảm so với kỳ trước', description: `${comparisonText(summary.netRevenue, previous.netRevenue)}. Kiểm tra lịch chiếu, phim chủ lực và các ngày có doanh thu thấp.` });
  if (Number(summary.occupancyRate) > 0 && Number(summary.occupancyRate) < 0.25) derivedSignals.push({ severity: 'WARNING', title: 'Công suất ghế đang thấp', description: `Tỷ lệ lấp đầy chỉ đạt ${percent(summary.occupancyRate)}. Nên rà soát khung giờ yếu và quy mô phòng chiếu.` });
  if (Number(summary.refundRate) >= 0.05) derivedSignals.push({ severity: 'HIGH', title: 'Tỷ lệ hoàn tiền cần chú ý', description: `${percent(summary.refundRate)} đơn hàng trong kỳ đã hoàn tiền. Kiểm tra nguyên nhân hủy và sự cố thanh toán.` });
  const insights = (report?.insights || []).filter(item => primaryCauseBelongsToCinema(item, selectedCinema));

  return (
    <div className="space-y-5">
      <SectionHeading icon={Search} eyebrow="Vì sao xảy ra?" title="Nguyên nhân và tín hiệu cần chú ý" description="Kết hợp phân tích tự động với so sánh kỳ trước để người quản lý biết nên kiểm tra ở đâu." />
      {insights.length ? (
        <section className="space-y-4">
          {insights.map(item => (
            <ConsolePanel key={item.id} className="p-5">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge value={item.severity} />
                    {item.confidenceScore != null ? <span className="text-[11px] text-zinc-600">Mức tin cậy {percent(item.confidenceScore)}</span> : null}
                  </div>
                  <h3 className="mt-3 font-black text-zinc-100">{localizeText(item.title, selectedCinema)}</h3>
                  <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-500">{localizeText(item.summary, selectedCinema)}</p>
                </div>
                <time className="text-xs text-zinc-600">{displayDate(item.statDate)}</time>
              </div>
              <div className="mt-5 rounded-xl border border-white/10 bg-black/20 p-4">
                <p className="flex items-center gap-2 text-xs font-black text-zinc-300"><Lightbulb size={15} className="text-orange-400" /> Kết luận nguyên nhân</p>
                <p className="mt-2 text-sm leading-6 text-zinc-500">{localizeText(item.rootCause, selectedCinema)}</p>
                {item.rootCauses?.length ? (
                  <div className="mt-4 grid gap-2 md:grid-cols-3">
                    {item.rootCauses.map(cause => (
                      <div key={`${item.id}-${cause.rank}`} className="rounded-lg bg-white/[0.035] px-3 py-3">
                        <p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Nguyên nhân #{cause.rank}</p>
                        <p className="mt-1 text-xs font-bold text-zinc-300">{CAUSE_LABELS[cause.causeType] || 'Biến động tại rạp'}</p>
                        <p className="mt-1 text-[11px] text-zinc-600">Mức đóng góp {percent(cause.contributionScore)}</p>
                      </div>
                    ))}
                  </div>
                ) : null}
              </div>
            </ConsolePanel>
          ))}
        </section>
      ) : derivedSignals.length ? (
        <section className="grid gap-4 md:grid-cols-2">
          {derivedSignals.map(signal => (
            <ConsolePanel key={signal.title} className="p-5">
              <Badge value={signal.severity} />
              <h3 className="mt-3 font-black text-zinc-100">{signal.title}</h3>
              <p className="mt-2 text-sm leading-6 text-zinc-500">{signal.description}</p>
              <p className="mt-4 text-[11px] text-zinc-600">Tín hiệu được tính từ số liệu của {selectedCinema.name}, không phải cảnh báo toàn chuỗi.</p>
            </ConsolePanel>
          ))}
        </section>
      ) : (
        <EmptyWorkspace title="Chưa thấy bất thường đáng chú ý" description="Kết quả tại rạp chưa tạo ra cảnh báo hoặc biến động lớn so với kỳ trước. Tiếp tục theo dõi khi có thêm dữ liệu." />
      )}
    </div>
  );
}

function ActionView({ report, selectedCinema, acting, onRecommendation, onAlert }) {
  const scopedInsightIds = new Set((report?.insights || [])
    .filter(item => primaryCauseBelongsToCinema(item, selectedCinema))
    .map(item => item.id));
  const recommendations = (report?.recommendations || []).filter(item => scopedInsightIds.has(item.insightId));
  const alerts = (report?.alerts || []).filter(item => scopedInsightIds.has(item.insightId));

  return (
    <div className="space-y-5">
      <SectionHeading icon={Target} eyebrow="Nên làm gì tiếp theo?" title="Danh sách việc cần xử lý" description={`Chỉ hiển thị khuyến nghị và cảnh báo được tạo từ dữ liệu của ${selectedCinema.name}.`} />
      <section className="grid gap-5 xl:grid-cols-[1.3fr_1fr]">
        <div className="space-y-4">
          <h3 className="flex items-center gap-2 text-sm font-black text-zinc-300"><Lightbulb size={17} className="text-orange-400" /> Khuyến nghị</h3>
          {recommendations.map(item => (
            <ConsolePanel key={item.id} className="p-5">
              <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-start">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-wider text-orange-400">{PRIORITY_LABELS[item.priority] || 'Ưu tiên thường'} · {SERVICE_LABELS[item.targetService] || 'Bộ phận liên quan'}</p>
                  <h4 className="mt-2 font-black text-zinc-100">{localizeText(item.title, selectedCinema)}</h4>
                  {item.confidenceScore != null ? <p className="mt-1 text-[11px] text-zinc-600">Mức tin cậy {percent(item.confidenceScore)}</p> : null}
                </div>
                <Badge value={item.status} />
              </div>
              <p className="mt-3 text-sm leading-6 text-zinc-500">{localizeText(item.description, selectedCinema)}</p>
              <div className="mt-4 rounded-xl bg-black/20 p-3 text-xs leading-5 text-zinc-500"><strong className="text-zinc-300">Tác động kỳ vọng:</strong> {localizeText(item.expectedImpact, selectedCinema)}</div>
              {item.status === 'PENDING' ? (
                <div className="mt-4 flex flex-wrap gap-2">
                  <button type="button" onClick={() => onRecommendation(item.id, 'ACCEPTED')} disabled={acting === `recommendation-${item.id}`} className="inline-flex items-center gap-2 rounded-lg bg-white px-3 py-2 text-xs font-black text-black disabled:opacity-50"><Check size={14} /> Nhận xử lý</button>
                  <button type="button" onClick={() => onRecommendation(item.id, 'DISMISSED')} disabled={acting === `recommendation-${item.id}`} className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-400 disabled:opacity-50"><X size={14} /> Bỏ qua</button>
                </div>
              ) : null}
              {item.status === 'ACCEPTED' ? <button type="button" onClick={() => onRecommendation(item.id, 'COMPLETED')} disabled={acting === `recommendation-${item.id}`} className="mt-4 inline-flex items-center gap-2 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-xs font-black text-emerald-300 disabled:opacity-50"><CheckCircle2 size={14} /> Đánh dấu hoàn tất</button> : null}
            </ConsolePanel>
          ))}
          {!recommendations.length ? <EmptyWorkspace title="Chưa có khuyến nghị cần quyết định" description="Hệ thống chưa tạo việc vận hành mới có nguyên nhân chính tại rạp trong kỳ đang xem." /> : null}
        </div>

        <div className="space-y-4">
          <h3 className="flex items-center gap-2 text-sm font-black text-zinc-300"><AlertTriangle size={17} className="text-red-400" /> Cảnh báo cần chú ý</h3>
          {alerts.map(item => (
            <article key={item.id} className="rounded-2xl border border-red-500/15 bg-red-500/5 p-5">
              <div className="flex items-center justify-between gap-3"><Badge value={item.severity} /><time className="text-[11px] text-zinc-600">{displayDate(item.createdAt)}</time></div>
              <h4 className="mt-3 text-sm font-black text-zinc-100">{localizeText(item.title, selectedCinema)}</h4>
              <p className="mt-2 text-xs leading-5 text-zinc-500">{localizeText(item.message, selectedCinema)}</p>
              {item.acknowledged ? <p className="mt-4 flex items-center gap-2 text-xs font-bold text-emerald-400"><CheckCircle2 size={15} /> Đã ghi nhận{item.acknowledgedBy ? ` bởi ${item.acknowledgedBy}` : ''}</p> : <button type="button" onClick={() => onAlert(item.id)} disabled={acting === `alert-${item.id}`} className="mt-4 rounded-lg border border-red-500/25 px-3 py-2 text-xs font-black text-red-300 disabled:opacity-50">Ghi nhận cảnh báo</button>}
            </article>
          ))}
          {!alerts.length ? <EmptyWorkspace title="Không có cảnh báo cần xử lý" description="Rạp hiện không có cảnh báo vận hành mà nguyên nhân chính thuộc phạm vi được phân công." /> : null}
        </div>
      </section>
    </div>
  );
}

export default function ManagerReportsPage() {
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const initialRange = useMemo(() => rangeFor(30), []);
  const [startDate, setStartDate] = useState(initialRange.startDate);
  const [endDate, setEndDate] = useState(initialRange.endDate);
  const [activeDays, setActiveDays] = useState(30);
  const [view, setView] = useState('result');
  const [state, setState] = useState({ loading: true, refreshing: false, error: '', report: null, previousReport: null });
  const [notice, setNotice] = useState('');
  const [acting, setActing] = useState('');

  const load = useCallback(async (refresh = false) => {
    if (!selectedCinemaId) return;
    if (startDate > endDate) {
      setState(current => ({ ...current, loading: false, refreshing: false, error: 'Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.' }));
      return;
    }
    setState(current => ({ ...current, loading: !refresh, refreshing: refresh, error: '' }));
    try {
      const previousRange = previousRangeFor(startDate, endDate);
      const [report, previousReport] = await Promise.all([
        managerCinemaService.getCinemaReport({ startDate, endDate, cinemaKey: selectedCinemaId }),
        managerCinemaService.getCinemaReport({ ...previousRange, cinemaKey: selectedCinemaId }),
      ]);
      setState({ loading: false, refreshing: false, error: '', report, previousReport });
    } catch (error) {
      setState(current => ({ ...current, loading: false, refreshing: false, error: error?.message || 'Không thể tải báo cáo của rạp.' }));
    }
  }, [endDate, selectedCinemaId, startDate]);

  useEffect(() => {
    load();
  }, [load]);

  const chooseRange = days => {
    const range = rangeFor(days);
    setActiveDays(days);
    setStartDate(range.startDate);
    setEndDate(range.endDate);
  };

  const updateCustomDate = (setter, value) => {
    setActiveDays(0);
    setter(value);
  };

  const handleRecommendation = async (id, status) => {
    setActing(`recommendation-${id}`);
    setNotice('');
    try {
      await managerCinemaService.updateRecommendation(id, status);
      setNotice(status === 'ACCEPTED' ? 'Đã nhận xử lý khuyến nghị.' : status === 'COMPLETED' ? 'Đã đánh dấu hoàn tất.' : 'Đã bỏ qua khuyến nghị.');
      await load(true);
    } catch (error) {
      setNotice(error?.message || 'Không thể cập nhật khuyến nghị.');
    } finally {
      setActing('');
    }
  };

  const handleAlert = async id => {
    setActing(`alert-${id}`);
    setNotice('');
    try {
      await managerCinemaService.acknowledgeAlert(id);
      setNotice('Đã ghi nhận cảnh báo.');
      await load(true);
    } catch (error) {
      setNotice(error?.message || 'Không thể ghi nhận cảnh báo.');
    } finally {
      setActing('');
    }
  };

  if (cinemaState.loading) return <p className="py-24 text-center text-sm font-bold text-zinc-500">Đang tải phạm vi rạp…</p>;
  if (!selectedCinema) return <EmptyWorkspace title="Chưa có rạp được phân công" description="Admin cần phân công rạp trước khi quản lý có thể xem báo cáo vận hành." />;

  const quality = state.report?.dataQuality || {};

  return (
    <main className="space-y-5 text-white">
      <HrHero
        context={`Báo cáo vận hành · ${selectedCinema.name}`}
        title="Trung tâm điều hành tại rạp"
        description="Theo dõi kết quả, tìm nguyên nhân và nhận việc xử lý trên cùng một màn hình. Mọi dữ liệu đều giới hạn ở rạp bạn được phân công."
        actions={<button type="button" onClick={() => load(true)} disabled={state.refreshing} className="inline-flex items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-black text-black disabled:opacity-50"><RefreshCw size={17} className={state.refreshing ? 'animate-spin' : ''} /> Làm mới</button>}
      />

      <ConsolePanel className="p-4">
        <div className="flex flex-col justify-between gap-4 2xl:flex-row 2xl:items-end">
          <div className="flex flex-wrap items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-orange-500/10 text-orange-400"><Building2 size={18} /></span>
            <div>
              <p className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Phạm vi cố định</p>
              <p className="mt-1 text-sm font-black text-zinc-200">{selectedCinema.name}</p>
            </div>
            <div className="ml-2 hidden h-9 w-px bg-white/10 sm:block" />
            <div className="flex items-center gap-2 text-xs text-zinc-500">
              <span className={`h-2 w-2 rounded-full ${quality.lastPipelineStatus === 'SUCCESS' ? 'bg-emerald-400' : 'bg-amber-400'}`} />
              Số liệu đến {displayDate(quality.lastCalculatedDate)}
              <Badge value={quality.freshnessStatus || 'NO_DATA'} />
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-2">
            <div className="flex rounded-xl border border-white/10 bg-black/20 p-1">
              {DATE_RANGES.map(option => <button key={option.days} type="button" onClick={() => chooseRange(option.days)} aria-pressed={activeDays === option.days} className={`rounded-lg px-3 py-2 text-xs font-bold ${activeDays === option.days ? 'bg-white text-black' : 'text-zinc-500 hover:text-zinc-200'}`}>{option.label}</button>)}
            </div>
            <label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Từ ngày<input aria-label="Từ ngày" type="date" value={startDate} max={endDate} onChange={event => updateCustomDate(setStartDate, event.target.value)} className="mt-1 block min-h-10 rounded-xl border border-white/10 bg-zinc-900 px-3 text-xs font-bold normal-case tracking-normal text-white" /></label>
            <label className="text-[10px] font-black uppercase tracking-wider text-zinc-600">Đến ngày<input aria-label="Đến ngày" type="date" value={endDate} min={startDate} onChange={event => updateCustomDate(setEndDate, event.target.value)} className="mt-1 block min-h-10 rounded-xl border border-white/10 bg-zinc-900 px-3 text-xs font-bold normal-case tracking-normal text-white" /></label>
          </div>
        </div>
      </ConsolePanel>

      <nav className="grid gap-3 md:grid-cols-3" aria-label="Ba nhóm báo cáo vận hành">
        {DECISION_VIEWS.map(item => (
          <button key={item.value} type="button" onClick={() => setView(item.value)} aria-pressed={view === item.value} className={`rounded-2xl border p-4 text-left transition ${view === item.value ? 'border-orange-500/40 bg-orange-500/10' : 'border-white/10 bg-white/[0.02] hover:border-white/20'}`}>
            <span className={`text-[10px] font-black uppercase tracking-wider ${view === item.value ? 'text-orange-400' : 'text-zinc-600'}`}>{item.eyebrow}</span>
            <strong className="mt-2 block text-sm font-black text-zinc-200">{item.label}</strong>
            <span className="mt-1 block text-xs text-zinc-600">{item.description}</span>
          </button>
        ))}
      </nav>

      {notice ? <div className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/[0.03] px-4 py-3 text-xs font-bold text-zinc-300"><CheckCircle2 size={16} className="text-emerald-400" /> {notice}</div> : null}
      {state.error ? <div className="rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-200">{state.error}</div> : null}
      {state.loading ? <div className="grid min-h-[40vh] place-items-center text-sm font-bold text-zinc-500"><span className="inline-flex items-center gap-2"><RefreshCw size={17} className="animate-spin" /> Đang tổng hợp và so sánh số liệu của rạp…</span></div> : null}
      {!state.loading && state.report ? (
        view === 'result' ? <ResultView report={state.report} previousReport={state.previousReport} selectedCinema={selectedCinema} />
          : view === 'cause' ? <CauseView report={state.report} previousReport={state.previousReport} selectedCinema={selectedCinema} />
            : <ActionView report={state.report} selectedCinema={selectedCinema} acting={acting} onRecommendation={handleRecommendation} onAlert={handleAlert} />
      ) : null}

      <div className="flex items-center gap-2 px-1 text-[11px] text-zinc-600"><Clock3 size={14} /> Báo cáo so sánh với kỳ liền trước có cùng số ngày; dữ liệu dự báo toàn chuỗi không được dùng thay cho dự báo của rạp.</div>
    </main>
  );
}
