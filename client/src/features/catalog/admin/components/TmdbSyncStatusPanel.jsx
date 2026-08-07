import { useCallback, useEffect, useRef, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  CircleStop,
  CloudDownload,
  Loader2,
  Play,
  RefreshCw,
  Search,
} from 'lucide-react';
import adminTmdbService from '@/features/catalog/admin/services/adminTmdbService';

const toDateInput = date => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const addDays = days => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return toDateInput(date);
};

const addYears = years => {
  const date = new Date();
  date.setFullYear(date.getFullYear() + years);
  return toDateInput(date);
};

const SCOPE_OPTIONS = [
  {
    value: 'FUTURE',
    label: 'Phim sắp phát hành',
    description: 'Chỉ nhập phim có ngày phát hành gốc trong tương lai.',
  },
  {
    value: 'PAST',
    label: 'Phim đã phát hành',
    description: 'Dùng khi cần đưa một nhóm phim cũ vào kho để khai thác lại.',
  },
  {
    value: 'RANGE',
    label: 'Khoảng ngày tự chọn',
    description: 'Nhập phim theo một khoảng ngày phát hành cụ thể.',
  },
  {
    value: 'ALL',
    label: 'Tất cả phim',
    description: 'Không lọc ngày phát hành. Nên đặt giới hạn nhỏ để tránh làm kho phim quá lớn.',
  },
];

const defaultDates = scope => {
  if (scope === 'FUTURE') return { releaseDateFrom: addDays(1), releaseDateTo: addYears(1) };
  if (scope === 'PAST') return { releaseDateFrom: addYears(-1), releaseDateTo: addDays(-1) };
  if (scope === 'RANGE') return { releaseDateFrom: addDays(-30), releaseDateTo: addYears(1) };
  return { releaseDateFrom: '', releaseDateTo: '' };
};

const statusView = status => {
  switch (status) {
    case 'RUNNING':
      return {
        title: 'Đang nhập phim',
        description: 'Hệ thống đang đọc dữ liệu TMDB và đưa phim phù hợp vào danh sách Chờ hoàn thiện.',
        tone: 'border-sky-500/30 bg-sky-500/[0.06]',
        icon: <Loader2 className="h-5 w-5 animate-spin text-sky-300" />,
      };
    case 'SUCCESS':
      return {
        title: 'Đã hoàn thành',
        description: 'Các phim mới đã được lưu ở trạng thái Chờ hoàn thiện để quản trị viên kiểm tra.',
        tone: 'border-emerald-500/30 bg-emerald-500/[0.06]',
        icon: <CheckCircle2 className="h-5 w-5 text-emerald-300" />,
      };
    case 'FAILED':
      return {
        title: 'Nhập phim không thành công',
        description: 'Vui lòng kiểm tra kết nối tới dịch vụ TMDB rồi chạy lại.',
        tone: 'border-red-500/30 bg-red-500/[0.06]',
        icon: <AlertTriangle className="h-5 w-5 text-red-300" />,
      };
    case 'STALE':
      return {
        title: 'Tiến trình có thể đã bị gián đoạn',
        description: 'Hệ thống không nhận được cập nhật trong một khoảng thời gian. Bạn có thể dừng rồi chạy lại từ đầu.',
        tone: 'border-amber-500/30 bg-amber-500/[0.06]',
        icon: <AlertTriangle className="h-5 w-5 text-amber-300" />,
      };
    case 'STOPPING':
      return {
        title: 'Đang dừng tiến trình',
        description: 'Hệ thống đang kết thúc tác vụ hiện tại và sẽ sớm trở về trạng thái Sẵn sàng.',
        tone: 'border-amber-500/30 bg-amber-500/[0.06]',
        icon: <Loader2 className="h-5 w-5 animate-spin text-amber-300" />,
      };
    case 'IDLE':
      return {
        title: 'Sẵn sàng nhập phim',
        description: 'Hiện không có tiến trình nhập phim nào đang chạy.',
        tone: 'border-zinc-700 bg-zinc-900/50',
        icon: <CloudDownload className="h-5 w-5 text-zinc-300" />,
      };
    default:
      return {
        title: 'Chưa có lần nhập phim nào',
        description: 'Hãy chọn phạm vi bên dưới để bắt đầu nhập phim từ TMDB.',
        tone: 'border-zinc-700 bg-zinc-900/50',
        icon: <CloudDownload className="h-5 w-5 text-zinc-300" />,
      };
  }
};

const errorMessage = error => {
  const message = error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message;
  if (!message) return 'Không thể thực hiện yêu cầu. Vui lòng thử lại.';

  const technicalConnectionError = /connection|network|socket|econn|before response|failed to fetch/i;
  if (technicalConnectionError.test(message)) {
    return 'Kết nối tới Movie Service hoặc nguồn TMDB đã bị gián đoạn. Vui lòng kiểm tra các dịch vụ rồi thử lại.';
  }
  if (/timeout|timed out/i.test(message)) {
    return 'Yêu cầu mất quá nhiều thời gian phản hồi. Vui lòng kiểm tra nguồn TMDB rồi thử lại.';
  }
  return message;
};

export default function TmdbSyncStatusPanel() {
  const [syncState, setSyncState] = useState(null);
  const [loading, setLoading] = useState(true);
  const [action, setAction] = useState('');
  const [notice, setNotice] = useState(null);
  const [tmdbId, setTmdbId] = useState('');
  const [form, setForm] = useState({
    scope: 'FUTURE',
    ...defaultDates('FUTURE'),
    maxMovies: 500,
  });
  const timerRef = useRef(null);

  const fetchSyncState = useCallback(async () => {
    try {
      const state = await adminTmdbService.getSyncState();
      setSyncState(state);
      setNotice(current => current?.type === 'error' ? null : current);
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchSyncState();
  }, [fetchSyncState]);

  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    const isBusy = syncState?.displayStatus === 'RUNNING' || syncState?.displayStatus === 'STOPPING';
    const delay = isBusy ? 1000 : 30000;
    timerRef.current = setTimeout(fetchSyncState, delay);
    return () => clearTimeout(timerRef.current);
  }, [fetchSyncState, syncState]);

  const changeScope = scope => {
    setForm(current => ({ ...current, scope, ...defaultDates(scope) }));
  };

  const payload = () => ({
    scope: form.scope,
    releaseDateFrom: form.scope === 'ALL' ? null : form.releaseDateFrom,
    releaseDateTo: form.scope === 'ALL' ? null : form.releaseDateTo,
    maxMovies: Number(form.maxMovies),
  });

  const runBulkImport = async reset => {
    setAction(reset ? 'reset' : 'start');
    setNotice(null);
    try {
      const response = reset
        ? await adminTmdbService.resetBulkSync(payload())
        : await adminTmdbService.startBulkSync(payload());
      setNotice({ type: 'success', message: response?.data || 'Đã bắt đầu nhập phim.' });
      setSyncState(current => ({ ...(current || {}), displayStatus: 'RUNNING' }));
      setTimeout(fetchSyncState, 800);
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setAction('');
    }
  };

  const stopImport = async () => {
    setAction('stop');
    setNotice(null);
    try {
      const response = await adminTmdbService.stopBulkSync();
      setNotice({ type: 'success', message: response?.data || 'Đã gửi yêu cầu dừng.' });
      setSyncState(current => ({
        ...(current || {}),
        displayStatus: 'STOPPING',
        message: 'Đã nhận yêu cầu dừng. Hệ thống đang kết thúc tác vụ hiện tại.',
      }));
      setTimeout(fetchSyncState, 300);
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setAction('');
    }
  };

  const importOneMovie = async event => {
    event.preventDefault();
    const parsedId = Number(tmdbId);
    if (!Number.isInteger(parsedId) || parsedId <= 0) {
      setNotice({ type: 'error', message: 'Vui lòng nhập mã TMDB hợp lệ.' });
      return;
    }
    setAction('single');
    setNotice(null);
    try {
      const response = await adminTmdbService.syncMovieById(parsedId);
      setNotice({ type: 'success', message: response?.data || 'Đã xử lý phim được chọn.' });
      setTmdbId('');
    } catch (error) {
      setNotice({ type: 'error', message: errorMessage(error) });
    } finally {
      setAction('');
    }
  };

  const view = statusView(syncState?.displayStatus);
  const isRunning = syncState?.displayStatus === 'RUNNING';
  const isStopping = syncState?.displayStatus === 'STOPPING';
  const isBusy = isRunning || isStopping;
  const selectedScope = SCOPE_OPTIONS.find(item => item.value === form.scope);

  return (
    <div className="space-y-5">
      <section className={`rounded-2xl border p-4 ${view.tone}`} aria-live="polite">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="flex items-start gap-3">
            <span className="rounded-xl border border-white/10 bg-black/20 p-2.5">{view.icon}</span>
            <div>
              <h3 className="text-sm font-bold text-zinc-100">{view.title}</h3>
              <p className="mt-1 text-xs leading-5 text-zinc-400">{syncState?.message || view.description}</p>
              <p className="mt-2 text-[11px] text-zinc-500">
                Tự động nhập khi khởi động dịch vụ: {' '}
                <strong className={syncState?.automaticSyncEnabled ? 'text-amber-300' : 'text-emerald-300'}>
                  {syncState?.automaticSyncEnabled ? 'Đang bật' : 'Đang tắt'}
                </strong>
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={fetchSyncState}
            disabled={loading}
            className="inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-zinc-700 bg-zinc-900 px-3 text-xs font-semibold text-zinc-300 hover:bg-zinc-800 disabled:opacity-50"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${loading ? 'animate-spin' : ''}`} />
            Làm mới
          </button>
        </div>

        {(syncState?.processedMovies > 0 || isBusy) && (
          <div className="mt-4 grid gap-2 sm:grid-cols-3">
            <div className="rounded-xl bg-black/20 p-3">
              <p className="text-[11px] text-zinc-500">Phim phù hợp đã xét</p>
              <p className="mt-1 text-lg font-black text-zinc-100">{syncState?.processedMovies || 0}</p>
            </div>
            <div className="rounded-xl bg-black/20 p-3">
              <p className="text-[11px] text-zinc-500">Phim mới đã nhập</p>
              <p className="mt-1 text-lg font-black text-emerald-300">{syncState?.importedMovies || 0}</p>
            </div>
            <div className="rounded-xl bg-black/20 p-3">
              <p className="text-[11px] text-zinc-500">Đã có hoặc không thể nhập</p>
              <p className="mt-1 text-lg font-black text-amber-300">{syncState?.skippedMovies || 0}</p>
            </div>
          </div>
        )}
      </section>

      {notice && (
        <div className={`rounded-xl border px-4 py-3 text-sm ${
          notice.type === 'error'
            ? 'border-red-500/30 bg-red-500/10 text-red-200'
            : 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
        }`}>
          {notice.message}
        </div>
      )}

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
        <div>
          <h3 className="text-sm font-bold text-white">Nhập nhiều phim theo phạm vi</h3>
          <p className="mt-1 text-xs leading-5 text-zinc-500">
            Phim mới luôn được lưu ở trạng thái Chờ hoàn thiện. Việc nhập phim không tự đưa phim ra phục vụ khách hàng.
          </p>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          {SCOPE_OPTIONS.map(option => (
            <button
              key={option.value}
              type="button"
              disabled={isBusy}
              onClick={() => changeScope(option.value)}
              className={`rounded-xl border p-3 text-left transition disabled:opacity-50 ${
                form.scope === option.value
                  ? 'border-orange-500 bg-orange-500/10'
                  : 'border-zinc-800 bg-zinc-900/40 hover:border-zinc-700'
              }`}
            >
              <span className={`block text-xs font-bold ${form.scope === option.value ? 'text-orange-300' : 'text-zinc-300'}`}>
                {option.label}
              </span>
              <span className="mt-1 block text-[11px] leading-4 text-zinc-500">{option.description}</span>
            </button>
          ))}
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-3">
          {form.scope !== 'ALL' && (
            <>
              <label className="text-xs text-zinc-400">
                Ngày phát hành từ
                <input
                  type="date"
                  value={form.releaseDateFrom}
                  disabled={isBusy}
                  onChange={event => setForm(current => ({ ...current, releaseDateFrom: event.target.value }))}
                  className="mt-1 h-10 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 text-zinc-200 outline-none focus:border-orange-500 disabled:opacity-50"
                />
              </label>
              <label className="text-xs text-zinc-400">
                Ngày phát hành đến
                <input
                  type="date"
                  value={form.releaseDateTo}
                  disabled={isBusy}
                  onChange={event => setForm(current => ({ ...current, releaseDateTo: event.target.value }))}
                  className="mt-1 h-10 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 text-zinc-200 outline-none focus:border-orange-500 disabled:opacity-50"
                />
              </label>
            </>
          )}
          <label className="text-xs text-zinc-400">
            Số phim tối đa
            <input
              type="number"
              min="1"
              max="5000"
              value={form.maxMovies}
              disabled={isBusy}
              onChange={event => setForm(current => ({ ...current, maxMovies: event.target.value }))}
              className="mt-1 h-10 w-full rounded-lg border border-zinc-800 bg-zinc-900 px-3 text-zinc-200 outline-none focus:border-orange-500 disabled:opacity-50"
            />
          </label>
        </div>
        <p className="mt-3 text-[11px] text-zinc-600">Phạm vi hiện tại: {selectedScope?.description}</p>

        <div className="mt-4 flex flex-wrap gap-2">
          {isStopping ? (
            <button
              type="button"
              disabled
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-amber-500/15 px-4 text-xs font-bold text-amber-200 opacity-70"
            >
              <Loader2 className="h-4 w-4 animate-spin" />
              Đang dừng tiến trình
            </button>
          ) : isRunning ? (
            <button
              type="button"
              onClick={stopImport}
              disabled={action === 'stop'}
              className="inline-flex h-10 items-center gap-2 rounded-xl bg-red-500/15 px-4 text-xs font-bold text-red-200 hover:bg-red-500/25 disabled:opacity-50"
            >
              {action === 'stop' ? <Loader2 className="h-4 w-4 animate-spin" /> : <CircleStop className="h-4 w-4" />}
              Dừng nhập phim
            </button>
          ) : (
            <>
              <button
                type="button"
                onClick={() => runBulkImport(false)}
                disabled={Boolean(action)}
                className="inline-flex h-10 items-center gap-2 rounded-xl bg-orange-500 px-4 text-xs font-black text-zinc-950 hover:bg-orange-400 disabled:opacity-50"
              >
                {action === 'start' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Play className="h-4 w-4" />}
                Bắt đầu nhập
              </button>
              {syncState && syncState.displayStatus !== 'NO_DATA' && (
                <button
                  type="button"
                  onClick={() => runBulkImport(true)}
                  disabled={Boolean(action)}
                  className="inline-flex h-10 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-xs font-bold text-zinc-300 hover:bg-zinc-800 disabled:opacity-50"
                >
                  {action === 'reset' ? <Loader2 className="h-4 w-4 animate-spin" /> : <RefreshCw className="h-4 w-4" />}
                  Chạy lại từ đầu
                </button>
              )}
            </>
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-950/60 p-4">
        <h3 className="text-sm font-bold text-white">Nhập một phim theo mã TMDB</h3>
        <p className="mt-1 text-xs leading-5 text-zinc-500">
          Phù hợp khi cần chiếu lại một phim cũ mà không muốn nhập cả kho phim quá khứ.
        </p>
        <form onSubmit={importOneMovie} className="mt-3 flex flex-col gap-2 sm:flex-row">
          <label className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
            <input
              type="number"
              min="1"
              value={tmdbId}
              onChange={event => setTmdbId(event.target.value)}
              placeholder="Nhập mã TMDB, ví dụ: 550"
              aria-label="Mã TMDB của phim"
              className="h-10 w-full rounded-xl border border-zinc-800 bg-zinc-900 pl-10 pr-3 text-sm text-zinc-200 outline-none placeholder:text-zinc-600 focus:border-orange-500"
            />
          </label>
          <button
            type="submit"
            disabled={action === 'single'}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-xl border border-orange-500/40 bg-orange-500/10 px-4 text-xs font-bold text-orange-200 hover:bg-orange-500/20 disabled:opacity-50"
          >
            {action === 'single' ? <Loader2 className="h-4 w-4 animate-spin" /> : <CloudDownload className="h-4 w-4" />}
            Nhập phim này
          </button>
        </form>
      </section>
    </div>
  );
}
