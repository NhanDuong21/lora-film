import { useCallback, useEffect, useState } from 'react';
import { CalendarPlus, History, Loader2, Plus, X } from 'lucide-react';
import { useOutletContext } from 'react-router-dom';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import { formatDate } from '@/utils/movieHelpers';
import { parseApiError } from '@/utils/apiErrorHandler';

const PERIOD_STATE = {
  UPCOMING: { label: 'Sắp tới', className: 'border-sky-500/30 bg-sky-500/10 text-sky-300' },
  ACTIVE: { label: 'Đang trong thời gian khai thác', className: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300' },
  ENDED: { label: 'Đã kết thúc', className: 'border-zinc-700 bg-zinc-900 text-zinc-400' },
};

const emptyForm = () => ({ startDate: '', endDate: '', note: '' });

const tomorrow = () => {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

export default function MovieExhibitionPeriodPanel({ movie, onUpdate }) {
  const { triggerToast } = useOutletContext() || {};
  const [periods, setPeriods] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm());
  const [error, setError] = useState('');
  const moviePublicId = movie?.publicId;
  const canCreatePeriod = movie?.status === 'DRAFT' || movie?.status === 'ENDED';

  const loadPeriods = useCallback(async () => {
    if (!moviePublicId) return;
    setLoading(true);
    try {
      setPeriods(await adminMovieService.getExhibitionPeriods(moviePublicId));
      setError('');
    } catch (requestError) {
      setError(parseApiError(requestError));
    } finally {
      setLoading(false);
    }
  }, [moviePublicId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadPeriods();
  }, [loadPeriods]);

  const createPeriod = async event => {
    event.preventDefault();
    if (!form.startDate) {
      setError('Vui lòng chọn ngày bắt đầu khai thác.');
      return;
    }
    if (form.startDate < tomorrow()) {
      setError('Ngày bắt đầu của đợt khai thác mới phải sau hôm nay.');
      return;
    }
    if (form.endDate && form.endDate < form.startDate) {
      setError('Ngày kết thúc không được trước ngày bắt đầu.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      await adminMovieService.createExhibitionPeriod(movie.publicId, {
        startDate: form.startDate,
        endDate: form.endDate || null,
        note: form.note.trim() || null,
      });
      triggerToast?.('Đã lập đợt khai thác mới cho phim.', 'success');
      setForm(emptyForm());
      setShowForm(false);
      await Promise.all([loadPeriods(), onUpdate?.()]);
    } catch (requestError) {
      setError(parseApiError(requestError));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-900/35" aria-labelledby="exhibition-period-title">
      <div className="flex flex-col gap-4 border-b border-zinc-800 p-5 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="rounded-xl bg-orange-500/10 p-2.5 text-orange-300">
            <CalendarPlus className="h-5 w-5" />
          </span>
          <div>
            <h3 id="exhibition-period-title" className="text-sm font-bold text-white">Các đợt khai thác tại rạp</h3>
            <p className="mt-1 max-w-2xl text-xs leading-5 text-zinc-500">
              Khi muốn chiếu lại phim cũ, hãy lập đợt mới. Ngày phát hành gốc của phim vẫn được giữ nguyên.
            </p>
          </div>
        </div>
        {canCreatePeriod && (
          <button
            type="button"
            onClick={() => {
              setShowForm(current => !current);
              setError('');
            }}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl bg-orange-500 px-4 text-xs font-black text-zinc-950 hover:bg-orange-400"
          >
            {showForm ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
            {showForm ? 'Đóng biểu mẫu' : 'Lập đợt khai thác mới'}
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={createPeriod} className="border-b border-zinc-800 bg-zinc-950/30 p-5">
          <div className="grid gap-3 md:grid-cols-2">
            <label className="text-xs text-zinc-400">
              Ngày bắt đầu khai thác <span className="text-red-400">*</span>
              <input
                type="date"
                min={tomorrow()}
                value={form.startDate}
                onChange={event => setForm(current => ({ ...current, startDate: event.target.value }))}
                className="mt-1 h-10 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 text-zinc-200 outline-none focus:border-orange-500"
              />
            </label>
            <label className="text-xs text-zinc-400">
              Ngày kết thúc khai thác
              <input
                type="date"
                value={form.endDate}
                onChange={event => setForm(current => ({ ...current, endDate: event.target.value }))}
                className="mt-1 h-10 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 text-zinc-200 outline-none focus:border-orange-500"
              />
            </label>
            <label className="text-xs text-zinc-400 md:col-span-2">
              Ghi chú
              <textarea
                rows="2"
                maxLength="500"
                value={form.note}
                onChange={event => setForm(current => ({ ...current, note: event.target.value }))}
                placeholder="Ví dụ: Chiếu lại nhân dịp kỷ niệm, tuần lễ phim kinh điển..."
                className="mt-1 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 py-2 text-sm text-zinc-200 outline-none placeholder:text-zinc-600 focus:border-orange-500"
              />
            </label>
          </div>
          {error && <p className="mt-3 text-xs text-red-300">{error}</p>}
          <div className="mt-4 flex justify-end">
            <button
              type="submit"
              disabled={saving}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-orange-500 px-4 text-xs font-black text-zinc-950 hover:bg-orange-400 disabled:opacity-50"
            >
              {saving ? <Loader2 className="h-4 w-4 animate-spin" /> : <CalendarPlus className="h-4 w-4" />}
              Lưu đợt khai thác
            </button>
          </div>
        </form>
      )}

      <div className="p-5">
        <div className="mb-3 flex items-center gap-2 text-xs font-semibold text-zinc-400">
          <History className="h-4 w-4" />
          Lịch sử khai thác
        </div>
        {loading ? (
          <div className="flex items-center gap-2 py-4 text-xs text-zinc-500">
            <Loader2 className="h-4 w-4 animate-spin" /> Đang tải lịch sử...
          </div>
        ) : periods.length === 0 ? (
          <p className="rounded-xl border border-dashed border-zinc-800 p-4 text-xs text-zinc-600">
            Chưa có lịch sử riêng. Thời gian hiện tại của phim sẽ được lưu lại khi bạn lập đợt khai thác mới.
          </p>
        ) : (
          <div className="space-y-2">
            {periods.map(period => {
              const state = PERIOD_STATE[period.periodState] || PERIOD_STATE.ENDED;
              return (
                <article key={period.publicId} className="flex flex-col gap-2 rounded-xl border border-zinc-800 bg-zinc-950/50 p-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold text-zinc-200">
                      {formatDate(period.startDate)} → {period.endDate ? formatDate(period.endDate) : 'Chưa xác định ngày kết thúc'}
                    </p>
                    {period.note && <p className="mt-1 text-xs text-zinc-500">{period.note}</p>}
                  </div>
                  <span className={`w-fit rounded-full border px-2.5 py-1 text-[11px] font-semibold ${state.className}`}>
                    {state.label}
                  </span>
                </article>
              );
            })}
          </div>
        )}
        {!showForm && error && <p className="mt-3 text-xs text-red-300">{error}</p>}
      </div>
    </section>
  );
}
