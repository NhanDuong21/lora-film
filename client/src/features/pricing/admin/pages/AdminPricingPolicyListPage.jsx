import { useCallback, useEffect, useState } from 'react';
import { CalendarRange, ChevronRight, Plus, RefreshCw, Search } from 'lucide-react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminPricingService from '../services/adminPricingService';

const statusStyle = {
  DRAFT: 'border-zinc-600 bg-zinc-700/20 text-zinc-300',
  ACTIVE: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300',
  INACTIVE: 'border-red-500/30 bg-red-500/10 text-red-300',
  EXPIRED: 'border-amber-500/30 bg-amber-500/10 text-amber-300',
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
      triggerToast?.(error.response?.data?.message || 'Không thể tải chính sách giá', 'error');
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
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return (
    <div className="min-h-full space-y-6 bg-zinc-950 text-white">
      <div className="flex flex-col gap-4 border-b border-zinc-800 pb-5 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.25em] text-amber-400">Pricing control</p>
          <h1 className="mt-2 text-3xl font-black">Chính sách giá suất chiếu</h1>
          <p className="mt-2 text-sm text-zinc-400">Quản lý phiên bản giá theo rạp, ngày hiệu lực và quy tắc ưu tiên.</p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/admin/pricing/create')}
          className="inline-flex items-center justify-center gap-2 rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-zinc-950 hover:bg-amber-400"
        >
          <Plus className="h-4 w-4" /> Tạo chính sách
        </button>
      </div>

      <div className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4 md:grid-cols-4">
        <label className="space-y-1 text-xs font-bold text-zinc-400">
          Rạp
          <select
            value={filters.cinema}
            onChange={event => setFilters(current => ({ ...current, cinema: event.target.value }))}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-zinc-200"
          >
            <option value="">Tất cả rạp</option>
            {cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}
          </select>
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">
          Trạng thái
          <select
            value={filters.status}
            onChange={event => setFilters(current => ({ ...current, status: event.target.value }))}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-zinc-200"
          >
            <option value="">Tất cả</option>
            <option value="DRAFT">Bản nháp</option>
            <option value="ACTIVE">Đang áp dụng</option>
            <option value="EXPIRED">Hết hiệu lực</option>
            <option value="INACTIVE">Ngừng áp dụng</option>
          </select>
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">
          Có hiệu lực ngày
          <input
            type="date"
            value={filters.effectiveDate}
            onChange={event => setFilters(current => ({ ...current, effectiveDate: event.target.value }))}
            className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-200"
          />
        </label>
        <div className="flex items-end">
          <button type="button" onClick={load} className="flex w-full items-center justify-center gap-2 rounded-xl border border-zinc-700 px-3 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800">
            <RefreshCw className="h-4 w-4" /> Làm mới
          </button>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30">
        {loading ? (
          <div className="p-12 text-center text-zinc-500">Đang tải chính sách giá…</div>
        ) : policies.length === 0 ? (
          <div className="flex flex-col items-center gap-3 p-12 text-zinc-500">
            <Search className="h-8 w-8" />
            <p>Không có chính sách phù hợp.</p>
          </div>
        ) : (
          <div className="divide-y divide-zinc-800">
            {policies.map(policy => (
              <button
                key={policy.publicId}
                type="button"
                onClick={() => navigate(`/admin/pricing/${policy.publicId}`)}
                className="grid w-full gap-3 p-5 text-left hover:bg-zinc-900 md:grid-cols-[1.5fr_1fr_1fr_auto] md:items-center"
              >
                <div>
                  <p className="font-black text-zinc-100">{policy.name}</p>
                  <p className="mt-1 font-mono text-xs text-zinc-500">{policy.publicId}</p>
                </div>
                <div>
                  <p className="text-sm font-bold text-zinc-300">{policy.cinemaName}</p>
                  <p className="text-xs text-zinc-500">Ưu tiên {policy.priority}</p>
                </div>
                <div className="flex items-center gap-2 text-sm text-zinc-400">
                  <CalendarRange className="h-4 w-4" />
                  {policy.effectiveFrom} → {policy.effectiveTo || 'Không giới hạn'}
                </div>
                <div className="flex items-center gap-3">
                  <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${statusStyle[policy.displayStatus] || statusStyle.DRAFT}`}>
                    {policy.displayStatus}
                  </span>
                  <ChevronRight className="h-4 w-4 text-zinc-600" />
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
