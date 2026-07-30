import { useCallback, useEffect, useState } from 'react';
import { CalendarDays, CheckCircle2, ChevronRight, CircleAlert, Plus, RefreshCw, Search, Ticket } from 'lucide-react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminPricingService from '../services/adminPricingService';

const statusStyle = {
  DRAFT: 'border-blue-500/30 bg-blue-500/10 text-blue-300',
  ACTIVE: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  INACTIVE: 'border-zinc-700 bg-zinc-800/70 text-zinc-400',
  EXPIRED: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
};

const statusLabel = {
  DRAFT: 'Đang soạn',
  ACTIVE: 'Đang áp dụng',
  INACTIVE: 'Đã ngừng',
  EXPIRED: 'Hết hiệu lực',
};

const statusDescription = {
  DRAFT: 'Bạn có thể chỉnh sửa trước khi dùng.',
  ACTIVE: 'Đang dùng để tính giá vé.',
  INACTIVE: 'Không còn được dùng cho suất mới.',
  EXPIRED: 'Đã qua thời gian áp dụng.',
};

const formatDate = value => {
  if (!value) return 'Không giới hạn';
  const [year, month, day] = String(value).slice(0, 10).split('-').map(Number);
  if (!year || !month || !day) return value;
  return new Intl.DateTimeFormat('vi-VN').format(new Date(Date.UTC(year, month - 1, day)));
};

export default function AdminPricingPolicyListPage() {
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};
  const [policies, setPolicies] = useState([]);
  const [cinemas, setCinemas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({ cinema: '', status: '', effectiveDate: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await adminPricingService.searchPolicies({
        ...filters,
        cinema: filters.cinema || undefined,
        status: filters.status || undefined,
        effectiveDate: filters.effectiveDate || undefined,
        page: 0,
        size: 100,
      });
      setPolicies(response?.data?.data || []);
    } catch (error) {
      triggerToast?.(error.response?.data?.message || 'Không thể tải bảng giá', 'error');
    } finally {
      setLoading(false);
    }
  }, [filters, triggerToast]);

  useEffect(() => {
    adminCinemaService.getCinemas({ page: 0, size: 100 })
      .then(response => setCinemas(response?.data?.data || []))
      .catch(() => setCinemas([]));
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- filters trigger a cancellable server-backed list refresh.
    load();
  }, [load]);

  const activeCount = policies.filter(policy => policy.displayStatus === 'ACTIVE').length;
  const draftCount = policies.filter(policy => policy.displayStatus === 'DRAFT').length;

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white">
      <header className="flex flex-col gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-brand-orange">Giá vé</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Bảng giá vé</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-zinc-400">
            Tạo một bảng giá dễ hiểu cho từng rạp. Khi mở bán, hệ thống sẽ tự dùng bảng giá phù hợp.
          </p>
        </div>
        <button type="button" onClick={() => navigate('/admin/pricing/create')} className="inline-flex min-h-11 items-center gap-2 self-start rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950 hover:bg-amber-400">
          <Plus className="h-4 w-4" aria-hidden="true" />
          Tạo bảng giá
        </button>
      </header>

      <section className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4">
          <div className="flex items-center gap-2 text-xs font-bold text-zinc-500"><CheckCircle2 className="h-4 w-4 text-emerald-300" aria-hidden="true" /> Đang áp dụng</div>
          <p className="mt-2 text-2xl font-black text-emerald-300">{activeCount}</p>
          <p className="mt-1 text-xs text-zinc-500">bảng giá đang được dùng</p>
        </div>
        <div className="rounded-2xl border border-zinc-800 bg-zinc-900/70 p-4">
          <div className="flex items-center gap-2 text-xs font-bold text-zinc-500"><CircleAlert className="h-4 w-4 text-blue-300" aria-hidden="true" /> Đang soạn</div>
          <p className="mt-2 text-2xl font-black text-blue-300">{draftCount}</p>
          <p className="mt-1 text-xs text-zinc-500">bảng giá chờ kiểm tra</p>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4 md:p-5" aria-labelledby="pricing-filter-heading">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 id="pricing-filter-heading" className="flex items-center gap-2 text-base font-black"><Search className="h-4 w-4 text-brand-orange" aria-hidden="true" /> Tìm bảng giá</h2>
            <p className="mt-1 text-xs text-zinc-500">Lọc theo rạp, trạng thái hoặc ngày áp dụng.</p>
          </div>
          <button type="button" onClick={() => setFilters({ cinema: '', status: '', effectiveDate: '' })} className="inline-flex min-h-9 items-center gap-2 rounded-lg border border-zinc-700 px-3 text-xs font-bold text-zinc-300 hover:bg-zinc-800"><RefreshCw className="h-3.5 w-3.5" aria-hidden="true" /> Xóa bộ lọc</button>
        </div>
        <div className="grid gap-3 lg:grid-cols-4">
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">Rạp<select value={filters.cinema} onChange={event => setFilters(current => ({ ...current, cinema: event.target.value }))} className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Tất cả rạp</option>{cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}</select></label>
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">Trạng thái<select value={filters.status} onChange={event => setFilters(current => ({ ...current, status: event.target.value }))} className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200"><option value="">Tất cả trạng thái</option><option value="DRAFT">Đang soạn</option><option value="ACTIVE">Đang áp dụng</option><option value="EXPIRED">Hết hiệu lực</option><option value="INACTIVE">Đã ngừng</option></select></label>
          <label className="space-y-1.5 text-xs font-bold text-zinc-400">Xem giá tại ngày<input type="date" value={filters.effectiveDate} onChange={event => setFilters(current => ({ ...current, effectiveDate: event.target.value }))} className="min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-200" /></label>
          <div className="flex items-end"><button type="button" onClick={load} className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-zinc-700 px-3 text-sm font-bold text-zinc-300 hover:bg-zinc-800"><RefreshCw className="h-4 w-4" aria-hidden="true" /> Làm mới</button></div>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30" aria-label="Danh sách bảng giá">
        <div className="flex items-center justify-between border-b border-zinc-800 px-4 py-4 md:px-5">
          <div><h2 className="text-base font-black">Các bảng giá</h2><p className="mt-1 text-xs text-zinc-500">Chọn một bảng để xem chi tiết hoặc chỉnh bản đang soạn.</p></div>
          <Ticket className="h-5 w-5 text-zinc-600" aria-hidden="true" />
        </div>
        {loading ? <div className="p-12 text-center text-sm text-zinc-500">Đang tải bảng giá…</div> : policies.length === 0 ? (
          <div className="flex flex-col items-center gap-3 px-6 py-16 text-center"><CalendarDays className="h-10 w-10 text-zinc-700" aria-hidden="true" /><h3 className="font-black text-zinc-200">Chưa có bảng giá</h3><p className="max-w-sm text-sm text-zinc-500">Tạo bảng giá đầu tiên để hệ thống có thể mở bán suất chiếu.</p><button type="button" onClick={() => navigate('/admin/pricing/create')} className="mt-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-zinc-950">Tạo bảng giá</button></div>
        ) : (
          <div className="divide-y divide-zinc-800">
            {policies.map(policy => (
              <button key={policy.publicId} type="button" onClick={() => navigate(`/admin/pricing/${policy.publicId}`)} className="grid w-full gap-4 p-5 text-left transition-colors hover:bg-zinc-900/70 md:grid-cols-[minmax(0,1.5fr)_1fr_1fr_auto] md:items-center">
                <div className="min-w-0"><p className="truncate font-black text-zinc-100">{policy.name}</p><p className="mt-1 text-xs text-zinc-500">{statusDescription[policy.displayStatus] || 'Bảng giá của rạp'}</p></div>
                <div><p className="text-sm font-bold text-zinc-300">{policy.cinemaName || 'Chưa chọn rạp'}</p><p className="mt-1 text-xs text-zinc-500">Áp dụng từ {formatDate(policy.effectiveFrom)}</p></div>
                <div className="flex items-center gap-2 text-sm text-zinc-400"><CalendarDays className="h-4 w-4" aria-hidden="true" /><span>Đến {formatDate(policy.effectiveTo)}</span></div>
                <div className="flex items-center gap-3"><span className={`rounded-full border px-2.5 py-1 text-xs font-bold ${statusStyle[policy.displayStatus] || statusStyle.DRAFT}`}>{statusLabel[policy.displayStatus] || 'Chưa xác định'}</span><ChevronRight className="h-4 w-4 text-zinc-600" aria-hidden="true" /></div>
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
