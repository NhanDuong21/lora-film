import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Building2,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  Clock3,
  Loader2,
  Plus,
  RefreshCw,
  ShieldCheck,
  UserRound,
  Wrench,
  X,
} from 'lucide-react';
import { getErrorMessage } from '@/utils/apiErrorHandler';

const STATUS_LABELS = {
  DRAFT: 'Đang soạn',
  OPEN_FOR_BOOKING: 'Đang mở bán',
  CLOSED: 'Đã đóng bán',
  CANCELLED: 'Đã hủy',
  FINISHED: 'Đã kết thúc',
};

const FILTERS = [
  ['active', 'Đang hiệu lực'],
  ['all', 'Tất cả'],
  ['cancelled', 'Đã hủy'],
];

const toLocalInput = date => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
};

const initialForm = roomId => {
  const start = new Date(Date.now() + 60 * 60 * 1000);
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
  return {
    auditoriumPublicId: roomId || '',
    startTime: toLocalInput(start),
    endTime: toLocalInput(end),
    reason: 'Bảo trì thiết bị phòng chiếu',
  };
};

const formatDateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(new Date(value))
  : 'Chưa xác định';

const formatTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(new Date(value))
  : '--:--';

const formatAccount = accountId => accountId ? `Tài khoản #${accountId}` : 'Chưa có dữ liệu';

const normalizeRoom = room => ({
  publicId: room.publicId,
  name: room.name || room.auditoriumName || 'Phòng chiếu',
  capacity: Number(room.capacity || 0),
  screenType: room.screenType || 'Tiêu chuẩn',
  status: room.status || room.auditoriumStatus || 'ACTIVE',
});

const windowPeriod = item => {
  if (item.status === 'CANCELLED') return 'cancelled';
  const now = Date.now();
  if (new Date(item.endTime).getTime() <= now) return 'past';
  if (new Date(item.startTime).getTime() <= now) return 'current';
  return 'upcoming';
};

const periodLabel = period => {
  if (period === 'current') return 'Đang diễn ra';
  if (period === 'upcoming') return 'Sắp tới';
  if (period === 'cancelled') return 'Đã hủy';
  return 'Đã qua';
};

const periodTone = period => {
  if (period === 'current') return 'bg-amber-500/10 text-amber-300';
  if (period === 'upcoming') return 'bg-blue-500/10 text-blue-300';
  return 'bg-zinc-800 text-zinc-400';
};

export default function MaintenanceWorkspace({
  rooms = [],
  loadWindows,
  createWindow,
  cancelWindow,
  previewImpact,
  initialRoomId = '',
  viewerRole = 'operator',
  onNotify,
}) {
  const normalizedRooms = useMemo(() => rooms.map(normalizeRoom), [rooms]);
  const [windows, setWindows] = useState([]);
  const [selectedRoomId, setSelectedRoomId] = useState(initialRoomId || normalizedRooms[0]?.publicId || '');
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState(() => initialForm(initialRoomId));
  const [impact, setImpact] = useState(null);
  const [acknowledged, setAcknowledged] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelAcknowledged, setCancelAcknowledged] = useState(false);
  const [listFilter, setListFilter] = useState('active');
  const [state, setState] = useState({
    loading: true,
    checking: false,
    saving: false,
    error: '',
    success: '',
  });

  useEffect(() => {
    if (initialRoomId) setSelectedRoomId(initialRoomId);
  }, [initialRoomId]);

  useEffect(() => {
    if (!selectedRoomId && normalizedRooms[0]?.publicId) {
      setSelectedRoomId(normalizedRooms[0].publicId);
    }
  }, [normalizedRooms, selectedRoomId]);

  const refresh = useCallback(async () => {
    if (typeof loadWindows !== 'function') return;
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const data = await loadWindows();
      setWindows(Array.isArray(data) ? data : []);
      setState(current => ({ ...current, loading: false }));
    } catch (error) {
      setState(current => ({
        ...current,
        loading: false,
        error: getErrorMessage(error, 'Không thể tải lịch bảo trì phòng chiếu.'),
      }));
    }
  }, [loadWindows]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const activeWindows = useMemo(() => windows.filter(item => (
    item.status === 'ACTIVE' && new Date(item.endTime).getTime() > Date.now()
  )), [windows]);
  const currentWindows = activeWindows.filter(item => new Date(item.startTime).getTime() <= Date.now());
  const selectedRoom = normalizedRooms.find(room => room.publicId === selectedRoomId) || normalizedRooms[0];
  const cancelRoom = cancelTarget
    ? normalizedRooms.find(room => room.publicId === cancelTarget.auditoriumPublicId)
    : null;

  const permissionNote = viewerRole === 'manager'
    ? 'Quyền của Manager áp dụng theo rạp được phân công, không phụ thuộc người tạo lịch. Bạn có thể hủy cả lịch do Admin tạo trong rạp này.'
    : viewerRole === 'admin'
      ? 'Quyền bảo trì áp dụng theo phạm vi rạp. Manager được phân công rạp vẫn có thể hủy lịch do Admin tạo.'
      : 'Quyền tạo và hủy lịch bảo trì áp dụng theo phạm vi rạp, không phụ thuộc người tạo lịch.';

  const roomStatus = roomId => {
    const current = currentWindows.find(item => item.auditoriumPublicId === roomId);
    if (current) return { label: 'Đang bảo trì', tone: 'amber', window: current };
    const upcoming = activeWindows.find(item => item.auditoriumPublicId === roomId);
    if (upcoming) return { label: 'Có lịch sắp tới', tone: 'blue', window: upcoming };
    return { label: 'Sẵn sàng', tone: 'emerald', window: null };
  };

  const visibleWindows = useMemo(() => windows.filter(item => {
    if (selectedRoomId && item.auditoriumPublicId !== selectedRoomId) return false;
    if (listFilter === 'all') return true;
    if (listFilter === 'cancelled') return item.status === 'CANCELLED';
    return item.status === 'ACTIVE' && new Date(item.endTime).getTime() > Date.now();
  }), [listFilter, selectedRoomId, windows]);

  const openForm = roomId => {
    const targetRoomId = roomId || selectedRoomId || normalizedRooms[0]?.publicId || '';
    setForm(initialForm(targetRoomId));
    setImpact(null);
    setAcknowledged(false);
    setState(current => ({ ...current, error: '', success: '' }));
    setFormOpen(true);
  };

  const closeForm = () => {
    if (state.saving) return;
    setFormOpen(false);
    setImpact(null);
    setAcknowledged(false);
    setState(current => ({ ...current, error: '' }));
  };

  const updateForm = (field, value) => {
    setForm(current => ({ ...current, [field]: value }));
    setImpact(null);
    setAcknowledged(false);
    setState(current => ({ ...current, error: '' }));
  };

  const payload = () => ({
    startTime: new Date(form.startTime).toISOString(),
    endTime: new Date(form.endTime).toISOString(),
    reason: form.reason.trim(),
  });

  const validateForm = () => {
    const start = new Date(form.startTime);
    const end = new Date(form.endTime);
    if (!form.auditoriumPublicId) return 'Vui lòng chọn phòng cần bảo trì.';
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return 'Vui lòng nhập đủ thời gian bắt đầu và kết thúc.';
    }
    if (start >= end) return 'Thời gian kết thúc phải sau thời gian bắt đầu.';
    if (start.getTime() < Date.now()) return 'Không thể tạo lịch bảo trì bắt đầu trong quá khứ.';
    if (form.reason.trim().length < 3) return 'Vui lòng ghi lý do để ca sau dễ bàn giao.';
    return '';
  };

  const checkImpact = async () => {
    const validationMessage = validateForm();
    if (validationMessage) {
      setState(current => ({ ...current, error: validationMessage }));
      return;
    }
    setState(current => ({ ...current, checking: true, error: '' }));
    try {
      const result = await previewImpact(form.auditoriumPublicId, payload());
      setImpact(result);
      setAcknowledged(false);
      setState(current => ({ ...current, checking: false }));
    } catch (error) {
      setState(current => ({
        ...current,
        checking: false,
        error: getErrorMessage(error, 'Không thể kiểm tra phạm vi ảnh hưởng.'),
      }));
    }
  };

  const submit = async event => {
    event.preventDefault();
    const validationMessage = validateForm();
    if (validationMessage) {
      setState(current => ({ ...current, error: validationMessage }));
      return;
    }
    if (!impact) {
      setState(current => ({ ...current, error: 'Vui lòng kiểm tra ảnh hưởng trước khi tạo lịch.' }));
      return;
    }
    if (!acknowledged) {
      setState(current => ({ ...current, error: 'Vui lòng xác nhận bạn đã hiểu phạm vi ảnh hưởng.' }));
      return;
    }

    setState(current => ({ ...current, saving: true, error: '' }));
    try {
      await createWindow(form.auditoriumPublicId, payload());
      setFormOpen(false);
      await refresh();
      const message = impact.affectedShowtimeCount > 0
        ? `Đã tạo lịch bảo trì. ${impact.affectedShowtimeCount} suất chiếu vẫn cần được xử lý riêng.`
        : 'Đã tạo lịch bảo trì. Không có suất chiếu nào bị ảnh hưởng.';
      setState(current => ({ ...current, saving: false, success: message }));
      onNotify?.(message, 'success');
    } catch (error) {
      setState(current => ({
        ...current,
        saving: false,
        error: getErrorMessage(error, 'Không thể tạo lịch bảo trì.'),
      }));
    }
  };

  const openCancelDialog = item => {
    setCancelTarget(item);
    setCancelAcknowledged(false);
    setState(current => ({ ...current, error: '', success: '' }));
  };

  const closeCancelDialog = () => {
    if (state.saving) return;
    setCancelTarget(null);
    setCancelAcknowledged(false);
    setState(current => ({ ...current, error: '' }));
  };

  const confirmCancel = async () => {
    if (!cancelTarget || !cancelAcknowledged) return;
    setState(current => ({ ...current, saving: true, error: '', success: '' }));
    try {
      await cancelWindow(cancelTarget);
      setCancelTarget(null);
      setCancelAcknowledged(false);
      await refresh();
      const message = 'Đã hủy lịch bảo trì. Phòng có thể được xếp suất trở lại trong khoảng thời gian này.';
      setState(current => ({ ...current, saving: false, success: message }));
      onNotify?.(message, 'success');
    } catch (error) {
      setState(current => ({
        ...current,
        saving: false,
        error: getErrorMessage(error, 'Không thể hủy lịch bảo trì.'),
      }));
    }
  };

  if (!normalizedRooms.length) {
    return (
      <div className="rounded-3xl border border-amber-500/20 bg-amber-500/5 p-10 text-center">
        <Building2 className="mx-auto text-amber-300" />
        <h2 className="mt-3 text-lg font-black">Chưa có phòng chiếu để vận hành</h2>
        <p className="mt-2 text-sm text-zinc-500">
          Hãy kiểm tra lại phạm vi rạp hoặc tạo phòng chiếu trước.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {[
          ['Tổng số phòng', normalizedRooms.length, Building2, 'text-white'],
          ['Sẵn sàng lúc này', normalizedRooms.length - currentWindows.length, CheckCircle2, 'text-emerald-300'],
          ['Đang bảo trì', currentWindows.length, Wrench, 'text-amber-300'],
          ['Lịch sắp tới', activeWindows.length - currentWindows.length, CalendarClock, 'text-blue-300'],
        ].map(([label, value, Icon, tone]) => (
          <article key={label} className="rounded-2xl border border-zinc-800 bg-zinc-900/30 p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[10px] font-black uppercase tracking-widest text-zinc-500">{label}</p>
                <p className={`mt-2 text-2xl font-black ${tone}`}>{value}</p>
              </div>
              <span className="grid h-10 w-10 place-items-center rounded-xl bg-zinc-950">
                <Icon className={`h-5 w-5 ${tone}`} />
              </span>
            </div>
          </article>
        ))}
      </section>

      <section className="rounded-3xl border border-zinc-800 bg-zinc-900/20 p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">
              Tình trạng từng phòng
            </p>
            <h2 className="mt-1 text-lg font-black">Chọn phòng để xem và bàn giao</h2>
            <p className="mt-1 text-xs leading-5 text-zinc-500">
              Màu trạng thái cho biết phòng đang phục vụ được hay đã có lịch bảo trì.
            </p>
          </div>
          <button
            type="button"
            onClick={() => openForm(selectedRoomId)}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950"
          >
            <Plus className="h-4 w-4" /> Tạo lịch bảo trì
          </button>
        </div>

        <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {normalizedRooms.map(room => {
            const status = roomStatus(room.publicId);
            const selected = room.publicId === selectedRoomId;
            return (
              <button
                key={room.publicId}
                type="button"
                onClick={() => setSelectedRoomId(room.publicId)}
                className={`rounded-2xl border p-4 text-left transition-all ${
                  selected
                    ? 'border-brand-orange bg-brand-orange/[0.08] ring-1 ring-brand-orange/20'
                    : 'border-zinc-800 bg-zinc-950/40 hover:border-zinc-700'
                }`}
              >
                <div className="flex items-start justify-between gap-3">
                  <span className={`grid h-9 w-9 place-items-center rounded-xl ${
                    status.tone === 'amber'
                      ? 'bg-amber-500/10 text-amber-300'
                      : status.tone === 'blue'
                        ? 'bg-blue-500/10 text-blue-300'
                        : 'bg-emerald-500/10 text-emerald-300'
                  }`}>
                    {status.tone === 'amber'
                      ? <Wrench className="h-4 w-4" />
                      : <CheckCircle2 className="h-4 w-4" />}
                  </span>
                  <span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${
                    status.tone === 'amber'
                      ? 'bg-amber-500/10 text-amber-300'
                      : status.tone === 'blue'
                        ? 'bg-blue-500/10 text-blue-300'
                        : 'bg-emerald-500/10 text-emerald-300'
                  }`}>
                    {status.label}
                  </span>
                </div>
                <h3 className="mt-4 truncate text-base font-black">{room.name}</h3>
                <p className="mt-1 text-xs text-zinc-500">{room.screenType} · {room.capacity} ghế</p>
                {status.window && (
                  <p className="mt-3 text-[11px] leading-5 text-zinc-400">
                    {status.tone === 'amber' ? 'Đến' : 'Bắt đầu'}{' '}
                    {formatDateTime(status.tone === 'amber' ? status.window.endTime : status.window.startTime)}
                  </p>
                )}
              </button>
            );
          })}
        </div>
      </section>

      {state.error && !formOpen && !cancelTarget && (
        <div role="alert" className="flex gap-3 rounded-2xl border border-red-500/25 bg-red-500/10 p-4 text-sm text-red-100">
          <AlertTriangle className="h-5 w-5 shrink-0" />
          <span>{state.error}</span>
        </div>
      )}
      {state.success && (
        <div role="status" className="flex gap-3 rounded-2xl border border-emerald-500/25 bg-emerald-500/10 p-4 text-sm text-emerald-100">
          <CheckCircle2 className="h-5 w-5 shrink-0" />
          <span>{state.success}</span>
        </div>
      )}

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900/20">
        <header className="flex flex-col gap-4 border-b border-zinc-800 p-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex items-start gap-3">
            <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand-orange/10 text-brand-orange">
              <CalendarClock className="h-5 w-5" />
            </span>
            <div>
              <h2 className="font-black">Lịch bảo trì của {selectedRoom?.name}</h2>
              <p className="mt-1 text-xs text-zinc-500">Theo dõi đầy đủ lịch đang hiệu lực và lịch đã hủy.</p>
              <p className="mt-2 max-w-3xl text-[11px] leading-5 text-blue-200/70">
                {permissionNote}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <div className="grid grid-cols-3 rounded-xl border border-zinc-800 bg-black/20 p-1">
              {FILTERS.map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  onClick={() => setListFilter(value)}
                  className={`rounded-lg px-3 py-2 text-[10px] font-black ${
                    listFilter === value ? 'bg-zinc-700 text-white' : 'text-zinc-500'
                  }`}
                >
                  {label}
                </button>
              ))}
            </div>
            <button
              type="button"
              onClick={refresh}
              disabled={state.loading}
              aria-label="Làm mới lịch bảo trì"
              className="rounded-xl border border-zinc-800 p-2.5 text-zinc-400 hover:text-white disabled:opacity-40"
            >
              <RefreshCw className={`h-4 w-4 ${state.loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </header>

        {state.loading ? (
          <div className="p-12 text-center">
            <Loader2 className="mx-auto h-6 w-6 animate-spin text-brand-orange" />
            <p className="mt-3 text-sm text-zinc-500">Đang cập nhật tình trạng phòng…</p>
          </div>
        ) : visibleWindows.length ? (
          <div className="divide-y divide-zinc-800">
            {visibleWindows.map(item => {
              const period = windowPeriod(item);
              return (
                <article
                  key={item.id}
                  className="grid gap-4 p-5 lg:grid-cols-[140px_1.1fr_1.35fr_1fr_auto] lg:items-center"
                >
                  <div>
                    <span className={`rounded-full px-2.5 py-1 text-[10px] font-black ${periodTone(period)}`}>
                      {periodLabel(period)}
                    </span>
                    <p className="mt-2 text-xs text-zinc-600">Mã bảo trì #{item.id}</p>
                  </div>
                  <div className="text-xs leading-5 text-zinc-300">
                    <p><span className="text-zinc-600">Từ:</span> {formatDateTime(item.startTime)}</p>
                    <p><span className="text-zinc-600">Đến:</span> {formatDateTime(item.endTime)}</p>
                  </div>
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-widest text-zinc-600">Lý do bảo trì</p>
                    <p className="mt-1 text-sm text-zinc-300">{item.reason || 'Không ghi lý do'}</p>
                  </div>
                  <div className="text-xs leading-5 text-zinc-400">
                    <p><span className="text-zinc-600">Tạo bởi:</span> {formatAccount(item.createdBy)}</p>
                    <p><span className="text-zinc-600">Tạo lúc:</span> {formatDateTime(item.createdAt)}</p>
                    {item.status === 'CANCELLED' && (
                      <p><span className="text-zinc-600">Hủy bởi:</span> {formatAccount(item.updatedBy)}</p>
                    )}
                  </div>
                  {item.status === 'ACTIVE' && new Date(item.endTime).getTime() > Date.now() ? (
                    <button
                      type="button"
                      disabled={state.saving}
                      onClick={() => openCancelDialog(item)}
                      className="rounded-xl border border-red-500/25 px-4 py-2 text-xs font-black text-red-300 hover:bg-red-500/10 disabled:opacity-40"
                    >
                      Hủy lịch bảo trì
                    </button>
                  ) : (
                    <span className="text-xs font-bold text-zinc-600">Không còn thao tác</span>
                  )}
                </article>
              );
            })}
          </div>
        ) : (
          <div className="p-12 text-center">
            <CalendarClock className="mx-auto text-zinc-700" />
            <p className="mt-3 text-sm font-bold text-zinc-400">Không có lịch phù hợp</p>
            <p className="mt-1 text-xs text-zinc-600">Phòng này chưa có lịch bảo trì trong nhóm đang xem.</p>
          </div>
        )}
      </section>

      {formOpen && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-4 backdrop-blur-sm">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="maintenance-form-title"
            className="max-h-[94vh] w-full max-w-4xl overflow-y-auto rounded-3xl border border-zinc-800 bg-zinc-950 shadow-2xl"
          >
            <header className="flex items-start justify-between gap-4 border-b border-zinc-800 p-5 md:p-6">
              <div>
                <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">
                  Bước 1 · Thông tin bảo trì
                </p>
                <h2 id="maintenance-form-title" className="mt-1 text-xl font-black">
                  Tạo lịch bảo trì phòng chiếu
                </h2>
                <p className="mt-1 text-xs leading-5 text-zinc-500">
                  Trong khoảng này, hệ thống sẽ chặn việc xếp thêm suất chiếu mới vào phòng.
                </p>
              </div>
              <button
                type="button"
                aria-label="Đóng biểu mẫu tạo lịch bảo trì"
                disabled={state.saving}
                onClick={closeForm}
                className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white"
              >
                <X className="h-5 w-5" />
              </button>
            </header>

            <form onSubmit={submit} className="space-y-5 p-5 md:p-6">
              <div className="grid gap-4 md:grid-cols-2">
                <label className="text-xs font-black text-zinc-400">
                  Phòng chiếu
                  <select
                    required
                    value={form.auditoriumPublicId}
                    onChange={event => updateForm('auditoriumPublicId', event.target.value)}
                    className="mt-2 min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 text-sm text-white outline-none focus:border-brand-orange"
                  >
                    {normalizedRooms.map(room => (
                      <option key={room.publicId} value={room.publicId}>
                        {room.name} · {room.capacity} ghế
                      </option>
                    ))}
                  </select>
                  <span className="mt-1 block text-[11px] font-normal leading-4 text-zinc-600">
                    Chọn đúng phòng cần ngừng phục vụ.
                  </span>
                </label>

                <label className="text-xs font-black text-zinc-400">
                  Lý do bảo trì
                  <input
                    required
                    minLength={3}
                    maxLength={255}
                    value={form.reason}
                    onChange={event => updateForm('reason', event.target.value)}
                    className="mt-2 min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 text-sm text-white outline-none focus:border-brand-orange"
                    placeholder="Ví dụ: Bảo trì máy chiếu"
                  />
                  <span className="mt-1 block text-[11px] font-normal leading-4 text-zinc-600">
                    Viết đủ rõ để ca sau biết phòng đang gặp vấn đề gì.
                  </span>
                </label>

                <label className="text-xs font-black text-zinc-400">
                  Bắt đầu
                  <input
                    required
                    lang="vi-VN"
                    type="datetime-local"
                    value={form.startTime}
                    onChange={event => updateForm('startTime', event.target.value)}
                    className="mt-2 min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 text-sm text-white outline-none focus:border-brand-orange"
                  />
                </label>

                <label className="text-xs font-black text-zinc-400">
                  Kết thúc dự kiến
                  <input
                    required
                    lang="vi-VN"
                    type="datetime-local"
                    value={form.endTime}
                    onChange={event => updateForm('endTime', event.target.value)}
                    className="mt-2 min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-900 px-3 text-sm text-white outline-none focus:border-brand-orange"
                  />
                </label>
              </div>

              <div className="flex gap-3 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3 text-xs leading-5 text-zinc-300">
                <Clock3 className="mt-0.5 h-4 w-4 shrink-0 text-blue-300" />
                <span>
                  <strong>Khoảng bảo trì:</strong> {formatDateTime(form.startTime)} – {formatDateTime(form.endTime)}
                </span>
              </div>

              <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4 md:p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-[10px] font-black uppercase tracking-[0.2em] text-blue-300">
                      Bước 2 · Kiểm tra ảnh hưởng
                    </p>
                    <h3 className="mt-1 text-sm font-black">Có suất chiếu nào cần xử lý riêng?</h3>
                  </div>
                  <button
                    type="button"
                    disabled={state.checking}
                    onClick={checkImpact}
                    className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-blue-500 px-4 text-xs font-black text-white disabled:opacity-50"
                  >
                    {state.checking
                      ? <Loader2 className="h-4 w-4 animate-spin" />
                      : <ShieldCheck className="h-4 w-4" />}
                    {state.checking ? 'Đang kiểm tra…' : impact ? 'Kiểm tra lại' : 'Kiểm tra ảnh hưởng'}
                  </button>
                </div>

                {!impact && !state.checking && (
                  <div className="mt-4 rounded-xl border border-dashed border-zinc-700 p-5 text-center text-xs leading-5 text-zinc-500">
                    Kiểm tra để biết có suất chiếu hoặc ghế khách nào cần xử lý trước giờ bảo trì.
                  </div>
                )}

                {impact && (
                  <div className="mt-4 space-y-4">
                    <div className={`rounded-2xl border p-4 ${
                      impact.affectedShowtimeCount > 0
                        ? 'border-amber-500/25 bg-amber-500/10'
                        : 'border-emerald-500/25 bg-emerald-500/10'
                    }`}>
                      <div className="flex gap-3">
                        {impact.affectedShowtimeCount > 0
                          ? <AlertTriangle className="h-5 w-5 shrink-0 text-amber-300" />
                          : <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-300" />}
                        <div>
                          <p className="text-sm font-black">
                            {impact.affectedShowtimeCount > 0
                              ? `Có ${impact.affectedShowtimeCount} suất chiếu cần xử lý`
                              : 'Không có suất chiếu nào bị ảnh hưởng'}
                          </p>
                          <p className="mt-1 text-xs leading-5 text-zinc-400">
                            {impact.affectedShowtimeCount > 0
                              ? `${impact.draftShowtimeCount || 0} đang soạn · ${impact.openForBookingCount || 0} đang mở bán · ${impact.occupiedSeatCount || 0} ghế đang được giữ hoặc đã bán.`
                              : 'Có thể tạo lịch bảo trì mà không làm gián đoạn lịch chiếu hiện tại.'}
                          </p>
                        </div>
                      </div>
                    </div>

                    {impact.affectedShowtimeCount > 0 && (
                      <div className="flex gap-2 rounded-xl border border-amber-500/25 bg-zinc-950 p-3 text-xs leading-5 text-amber-100">
                        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                        <span>
                          <strong>Tạo lịch bảo trì không tự động hủy hoặc đóng bán các suất dưới đây.</strong>{' '}
                          Hãy đóng bán, đổi phòng hoặc xử lý khách bằng luồng điều phối suất chiếu.
                        </span>
                      </div>
                    )}

                    {!impact.bookingDataComplete && (
                      <div className="flex gap-2 rounded-xl border border-red-500/20 bg-red-500/5 p-3 text-xs leading-5 text-red-100">
                        <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
                        Chưa kiểm tra đầy đủ ghế đang giữ hoặc đã mua. Hãy kiểm tra đơn đặt vé trước khi tạo lịch.
                      </div>
                    )}

                    {impact.showtimes?.length > 0 && (
                      <div className="max-h-56 space-y-2 overflow-y-auto">
                        {impact.showtimes.map(showtime => (
                          <article
                            key={showtime.showtimePublicId}
                            className="grid gap-2 rounded-xl border border-zinc-800 bg-zinc-950 p-3 sm:grid-cols-[1fr_auto] sm:items-center"
                          >
                            <div>
                              <p className="text-sm font-black">{showtime.movieTitle}</p>
                              <p className="mt-1 text-xs text-zinc-500">
                                {formatDateTime(showtime.startTime)} – {formatTime(showtime.endTime)}
                              </p>
                            </div>
                            <div className="flex flex-wrap items-center gap-2">
                              <span className="rounded-lg bg-zinc-800 px-2 py-1 text-[10px] font-black text-zinc-300">
                                {STATUS_LABELS[showtime.status] || showtime.status}
                              </span>
                              <span className="rounded-lg bg-amber-500/10 px-2 py-1 text-[10px] font-black text-amber-200">
                                {showtime.bookingDataAvailable
                                  ? `${showtime.occupiedSeatCount} ghế có khách`
                                  : 'Chưa kiểm tra được ghế'}
                              </span>
                            </div>
                          </article>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </section>

              {impact && (
                <section className="rounded-2xl border border-brand-orange/25 bg-brand-orange/[0.06] p-4">
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">
                    Bước 3 · Xác nhận trước khi tạo
                  </p>
                  <label className="mt-3 flex cursor-pointer items-start gap-3 rounded-xl border border-zinc-800 bg-zinc-950/70 p-3 text-xs leading-5 text-zinc-200">
                    <input
                      type="checkbox"
                      checked={acknowledged}
                      onChange={event => setAcknowledged(event.target.checked)}
                      className="mt-0.5 h-4 w-4 accent-orange-500"
                    />
                    <span>
                      {impact.affectedShowtimeCount > 0
                        ? `Tôi đã xem ${impact.affectedShowtimeCount} suất bị ảnh hưởng, hiểu rằng hệ thống không tự động hủy chúng và sẽ xử lý riêng trước khi phòng ngừng phục vụ.`
                        : 'Tôi đã kiểm tra phòng và khoảng thời gian, đồng thời xác nhận có thể tạo lịch bảo trì này.'}
                    </span>
                  </label>
                </section>
              )}

              {state.error && (
                <div role="alert" className="flex gap-2 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-xs text-red-100">
                  <AlertTriangle className="h-4 w-4 shrink-0" /> {state.error}
                </div>
              )}

              <div className="flex flex-col-reverse gap-3 border-t border-zinc-800 pt-5 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  disabled={state.saving}
                  onClick={closeForm}
                  className="min-h-11 rounded-xl border border-zinc-700 px-5 text-sm font-black text-zinc-300"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={state.saving || !impact || !acknowledged}
                  className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-5 text-sm font-black text-zinc-950 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {state.saving
                    ? <Loader2 className="h-4 w-4 animate-spin" />
                    : <ChevronRight className="h-4 w-4" />}
                  {state.saving
                    ? 'Đang tạo…'
                    : impact?.affectedShowtimeCount > 0
                      ? `Tạo lịch bảo trì · ${impact.affectedShowtimeCount} suất cần xử lý`
                      : 'Tạo lịch bảo trì'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {cancelTarget && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/80 p-4 backdrop-blur-sm">
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="cancel-maintenance-title"
            className="w-full max-w-xl overflow-hidden rounded-3xl border border-red-500/25 bg-zinc-950 shadow-2xl"
          >
            <header className="flex items-start justify-between gap-4 border-b border-zinc-800 p-5 md:p-6">
              <div className="flex items-start gap-3">
                <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-red-500/10 text-red-300">
                  <AlertTriangle className="h-5 w-5" />
                </span>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-[0.2em] text-red-300">
                    Hủy khoảng bảo trì
                  </p>
                  <h2 id="cancel-maintenance-title" className="mt-1 text-xl font-black">
                    Xác nhận hủy lịch bảo trì
                  </h2>
                  <p className="mt-1 text-xs leading-5 text-zinc-500">
                    Đây là hủy lịch bảo trì, không phải hủy suất chiếu.
                  </p>
                </div>
              </div>
              <button
                type="button"
                aria-label="Đóng hộp xác nhận hủy lịch bảo trì"
                disabled={state.saving}
                onClick={closeCancelDialog}
                className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white"
              >
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="space-y-4 p-5 md:p-6">
              <section className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4 sm:grid-cols-2">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-600">Phòng chiếu</p>
                  <p className="mt-1 text-sm font-black text-white">{cancelRoom?.name || 'Phòng chiếu'}</p>
                </div>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-600">Mã bảo trì</p>
                  <p className="mt-1 text-sm font-black text-white">#{cancelTarget.id}</p>
                </div>
                <div className="sm:col-span-2">
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-600">Khoảng thời gian</p>
                  <p className="mt-1 text-sm text-zinc-200">
                    {formatDateTime(cancelTarget.startTime)} – {formatDateTime(cancelTarget.endTime)}
                  </p>
                </div>
                <div className="sm:col-span-2">
                  <p className="text-[10px] font-black uppercase tracking-widest text-zinc-600">Lý do bảo trì</p>
                  <p className="mt-1 text-sm text-zinc-300">{cancelTarget.reason || 'Không ghi lý do'}</p>
                </div>
                <div className="sm:col-span-2 flex items-center gap-2 border-t border-zinc-800 pt-3 text-xs text-zinc-400">
                  <UserRound className="h-4 w-4 text-zinc-500" />
                  Tạo bởi {formatAccount(cancelTarget.createdBy)} lúc {formatDateTime(cancelTarget.createdAt)}
                </div>
              </section>

              <div className="space-y-2 rounded-2xl border border-red-500/20 bg-red-500/[0.06] p-4 text-xs leading-5 text-red-100">
                <p className="font-black">Sau khi hủy lịch bảo trì:</p>
                <ul className="list-disc space-y-1 pl-5 text-red-100/80">
                  <li>Phòng có thể được xếp suất trở lại trong khoảng thời gian trên.</li>
                  <li>Hệ thống không tự động tạo lại hoặc mở bán các suất chiếu trước đó.</li>
                </ul>
              </div>

              <div className="flex gap-2 rounded-xl border border-blue-500/20 bg-blue-500/[0.06] p-3 text-xs leading-5 text-blue-100/80">
                <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-blue-300" />
                {permissionNote}
              </div>

              <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-zinc-800 bg-zinc-900/50 p-3 text-xs leading-5 text-zinc-200">
                <input
                  type="checkbox"
                  checked={cancelAcknowledged}
                  onChange={event => setCancelAcknowledged(event.target.checked)}
                  className="mt-0.5 h-4 w-4 accent-red-500"
                />
                <span>
                  Tôi đã kiểm tra và xác nhận phòng có thể được đưa trở lại kế hoạch vận hành trong khoảng này.
                </span>
              </label>

              {state.error && (
                <div role="alert" className="flex gap-2 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-xs text-red-100">
                  <AlertTriangle className="h-4 w-4 shrink-0" /> {state.error}
                </div>
              )}

              <div className="flex flex-col-reverse gap-3 border-t border-zinc-800 pt-5 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  disabled={state.saving}
                  onClick={closeCancelDialog}
                  className="min-h-11 rounded-xl border border-zinc-700 px-5 text-sm font-black text-zinc-300"
                >
                  Giữ lịch bảo trì
                </button>
                <button
                  type="button"
                  disabled={state.saving || !cancelAcknowledged}
                  onClick={confirmCancel}
                  className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-red-500 px-5 text-sm font-black text-white hover:bg-red-400 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {state.saving && <Loader2 className="h-4 w-4 animate-spin" />}
                  {state.saving ? 'Đang hủy…' : 'Hủy lịch bảo trì'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
