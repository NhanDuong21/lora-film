import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  AlertTriangle,
  Armchair,
  CheckCircle2,
  Clock3,
  Loader2,
  LockKeyhole,
  RefreshCw,
  ShieldAlert,
  UnlockKeyhole,
  X,
} from 'lucide-react';
import apiClient from '@/services/apiClient';
import { getErrorMessage } from '@/utils/apiErrorHandler';
import { buildSeatUnits } from '@/features/booking/customer/utils/seatUnits';
import { seatTypePresentation } from '@/features/booking/customer/utils/seatPresentation';

const REASON_PRESETS = [
  'Ghế hỏng, cần kiểm tra',
  'Dành cho kỹ thuật hoặc vận hành',
  'Tạm ngưng để bảo đảm an toàn',
  'Theo yêu cầu của sự kiện',
];

const formatDateTime = (value, timezone) => {
  if (!value) return 'Chưa xác định';
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: timezone || undefined,
    dateStyle: 'full',
    timeStyle: 'short',
  }).format(new Date(value));
};

const loadAvailability = async showtimePublicId => {
  try {
    const response = await apiClient.get(
      `/api/seat-reservations/showtimes/${encodeURIComponent(showtimePublicId)}/availability`,
    );
    return response.data;
  } catch {
    return null;
  }
};

const seatTone = ({ seat, occupied, selected, mode }) => {
  if (selected) return 'border-brand-orange bg-brand-orange text-zinc-950 ring-2 ring-orange-300/40';
  if (occupied?.status === 'BOOKED') return 'cursor-not-allowed border-zinc-700 bg-zinc-800 text-zinc-500 opacity-60';
  if (occupied?.status === 'HELD') return 'cursor-not-allowed border-fuchsia-500/40 bg-fuchsia-950/60 text-fuchsia-200';
  if (seat.operationalStatus !== 'ACTIVE') return 'cursor-not-allowed border-red-500/30 bg-red-950/30 text-red-300 opacity-65';
  if (seat.blocked) {
    return mode === 'release'
      ? 'border-amber-400/60 bg-amber-500/15 text-amber-100 hover:bg-amber-500/25'
      : 'cursor-not-allowed border-amber-500/35 bg-amber-500/10 text-amber-200';
  }
  const typeTone = seatTypePresentation(seat.seatTypeCode).className;
  return mode === 'block'
    ? `${typeTone} hover:ring-2 hover:ring-brand-orange/60 hover:brightness-125`
    : `${typeTone} cursor-not-allowed opacity-35`;
};

export default function ShowtimeSeatControlDrawer({
  showtimePublicId,
  seatControlApi,
  onClose,
}) {
  const [layout, setLayout] = useState(null);
  const [availability, setAvailability] = useState(null);
  const [mode, setMode] = useState('block');
  const [selectedIds, setSelectedIds] = useState([]);
  const [reason, setReason] = useState(REASON_PRESETS[0]);
  const [state, setState] = useState({ loading: true, saving: false, error: '', success: '' });

  const load = useCallback(async () => {
    if (!showtimePublicId || !seatControlApi) return;
    setState(current => ({ ...current, loading: true, error: '' }));
    try {
      const [seatControl, seatAvailability] = await Promise.all([
        seatControlApi.getSeatControl(showtimePublicId),
        loadAvailability(showtimePublicId),
      ]);
      setLayout(seatControl);
      setAvailability(seatAvailability);
      setSelectedIds([]);
      setState(current => ({ ...current, loading: false }));
    } catch (error) {
      setState(current => ({
        ...current,
        loading: false,
        error: getErrorMessage(error, 'Không thể tải sơ đồ ghế của suất chiếu.'),
      }));
    }
  }, [seatControlApi, showtimePublicId]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    const onKeyDown = event => {
      if (event.key === 'Escape' && !state.saving) onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose, state.saving]);

  const occupiedBySeat = useMemo(() => new Map(
    (availability?.occupiedSeats || []).map(item => [item.seatPublicId, item]),
  ), [availability]);

  const seatUnits = useMemo(() => buildSeatUnits(
    (layout?.seats || []).map(seat => ({
      ...seat,
      seatType: seat.seatTypeCode,
      priced: true,
      price: 1,
      sellable: true,
      blockedForShowtime: seat.blocked,
      reservationStatus: occupiedBySeat.get(seat.publicId)?.status,
    })),
  ), [layout, occupiedBySeat]);

  const rows = useMemo(() => {
    const grouped = new Map();
    seatUnits.forEach(seatUnit => {
      const row = seatUnit.rowLabel || '?';
      if (!grouped.has(row)) grouped.set(row, []);
      grouped.get(row).push(seatUnit);
    });
    return Array.from(grouped.entries())
      .sort((left, right) => left[0].localeCompare(right[0], 'vi', { numeric: true }))
      .map(([row, units]) => [
        row,
        units.sort((left, right) => (
          Number(left.positionColumn ?? left.seatNumber ?? 0)
          - Number(right.positionColumn ?? right.seatNumber ?? 0)
        )),
      ]);
  }, [seatUnits]);

  const maxColumn = useMemo(() => Math.max(
    1,
    ...seatUnits.map(seatUnit => (
      Number(seatUnit.positionColumn ?? seatUnit.seatNumber ?? 0)
      + Number(seatUnit.columnSpan || 1)
    )),
  ), [seatUnits]);

  const seatTypes = useMemo(() => Array.from(new Map(
    (layout?.seats || []).map(seat => [seat.seatTypeCode, seat]),
  ).values()).sort((left, right) => (
    seatTypePresentation(left.seatTypeCode).order
    - seatTypePresentation(right.seatTypeCode).order
  )), [layout]);

  const selectedSeats = useMemo(() => (
    (layout?.seats || []).filter(seat => selectedIds.includes(seat.publicId))
  ), [layout, selectedIds]);

  const switchMode = nextMode => {
    setMode(nextMode);
    setSelectedIds([]);
    setState(current => ({ ...current, error: '', success: '' }));
  };

  const canSelect = seat => {
    if (!layout?.editable || seat.operationalStatus !== 'ACTIVE') return false;
    if (occupiedBySeat.has(seat.publicId)) return false;
    return mode === 'block' ? !seat.blocked : seat.blocked;
  };

  const canSelectUnit = seatUnit => (
    seatUnit.seats.some(canSelect)
    && seatUnit.seats.every(seat => (
      layout?.editable
      && seat.operationalStatus === 'ACTIVE'
      && !occupiedBySeat.has(seat.publicId)
    ))
  );

  const toggleSeat = seatUnit => {
    if (!canSelectUnit(seatUnit)) return;
    const unitIds = seatUnit.seats.map(seat => seat.publicId);
    const shouldRemove = unitIds.every(id => selectedIds.includes(id));
    setSelectedIds(current => shouldRemove
      ? current.filter(id => !unitIds.includes(id))
      : Array.from(new Set([...current, ...unitIds])));
    setState(current => ({ ...current, error: '', success: '' }));
  };

  const submit = async () => {
    if (!selectedIds.length || state.saving) return;
    if (mode === 'block' && reason.trim().length < 3) {
      setState(current => ({ ...current, error: 'Vui lòng chọn hoặc nhập lý do khóa ghế.' }));
      return;
    }
    setState(current => ({ ...current, saving: true, error: '', success: '' }));
    try {
      const nextLayout = mode === 'block'
        ? await seatControlApi.blockSeats(showtimePublicId, selectedIds, reason.trim())
        : await seatControlApi.releaseBlockedSeats(
          showtimePublicId,
          selectedIds,
          'Đã kiểm tra và mở lại ghế cho khách đặt vé',
        );
      setLayout(nextLayout);
      setAvailability(await loadAvailability(showtimePublicId));
      setSelectedIds([]);
      setState({
        loading: false,
        saving: false,
        error: '',
        success: mode === 'block'
          ? 'Đã khóa ghế. Khách sẽ không thể chọn các ghế này trong suất chiếu.'
          : 'Đã mở lại ghế. Khách có thể chọn ghế nếu ghế chưa được người khác giữ.',
      });
    } catch (error) {
      setState(current => ({
        ...current,
        saving: false,
        error: getErrorMessage(error, mode === 'block'
          ? 'Không thể khóa các ghế đã chọn.'
          : 'Không thể mở lại các ghế đã chọn.'),
      }));
      setAvailability(await loadAvailability(showtimePublicId));
    }
  };

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/80 p-0 backdrop-blur-sm sm:p-4 lg:p-6" onMouseDown={event => event.target === event.currentTarget && !state.saving && onClose()}>
      <aside role="dialog" aria-modal="true" aria-labelledby="seat-control-title" className="flex h-full w-full max-w-7xl flex-col overflow-hidden border-zinc-800 bg-zinc-950 text-white shadow-2xl sm:h-[calc(100dvh-2rem)] sm:rounded-3xl sm:border lg:h-[calc(100dvh-3rem)]">
        <header className="flex shrink-0 items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4 md:px-7">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Khóa ghế theo từng suất chiếu</p>
            <h2 id="seat-control-title" className="mt-1 text-xl font-black md:text-2xl">{layout?.movieTitle || 'Quản lý ghế'}</h2>
            {layout && <p className="mt-1 text-xs text-zinc-400">
              {layout.auditoriumName} · {formatDateTime(layout.startTime, layout.cinemaTimezone)}
            </p>}
          </div>
          <button type="button" disabled={state.saving} onClick={onClose} aria-label="Đóng quản lý ghế" className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white disabled:opacity-40"><X className="h-5 w-5" /></button>
        </header>

        {state.loading ? (
          <div className="grid flex-1 place-items-center"><div className="text-center"><Loader2 className="mx-auto h-8 w-8 animate-spin text-brand-orange" /><p className="mt-3 text-sm font-bold text-zinc-400">Đang tải tình trạng ghế…</p></div></div>
        ) : state.error && !layout ? (
          <div className="grid flex-1 place-items-center p-6"><div className="max-w-md rounded-2xl border border-red-500/25 bg-red-500/10 p-6 text-center"><ShieldAlert className="mx-auto text-red-300" /><p className="mt-3 text-sm text-red-100">{state.error}</p><button type="button" onClick={load} className="mt-4 rounded-xl bg-white px-4 py-2 text-xs font-black text-black">Thử tải lại</button></div></div>
        ) : layout && (
          <>
            <div className="flex-1 overflow-y-auto p-4 md:p-7">
              <section className="grid gap-4 xl:grid-cols-[1fr_300px]">
                <div className="space-y-4">
                  {!layout.editable && <div className="flex gap-3 rounded-2xl border border-amber-500/25 bg-amber-500/10 p-4"><AlertTriangle className="h-5 w-5 shrink-0 text-amber-300" /><div><p className="text-sm font-black text-amber-100">Chỉ được xem sơ đồ ghế</p><p className="mt-1 text-xs leading-5 text-amber-100/70">{layout.editabilityMessage}</p></div></div>}

                  <div className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                      <div>
                        <h3 className="text-sm font-black">Bạn muốn làm gì?</h3>
                        <p className="mt-1 text-xs text-zinc-500">Thao tác chỉ áp dụng cho suất chiếu này, không thay đổi sơ đồ ghế của phòng.</p>
                      </div>
                      <div className="grid grid-cols-2 rounded-xl border border-zinc-800 bg-black/30 p-1">
                        <button type="button" onClick={() => switchMode('block')} className={`flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 text-xs font-black ${mode === 'block' ? 'bg-amber-500 text-zinc-950' : 'text-zinc-400'}`}><LockKeyhole className="h-4 w-4" /> Khóa ghế</button>
                        <button type="button" onClick={() => switchMode('release')} className={`flex min-h-10 items-center justify-center gap-2 rounded-lg px-4 text-xs font-black ${mode === 'release' ? 'bg-emerald-500 text-zinc-950' : 'text-zinc-400'}`}><UnlockKeyhole className="h-4 w-4" /> Mở lại ghế</button>
                      </div>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-2 rounded-2xl border border-zinc-800 bg-zinc-900/30 p-3 text-[10px] font-bold text-zinc-300">
                    <span className="self-center px-1 font-black uppercase tracking-wider text-zinc-500">Loại ghế</span>
                    {seatTypes.map(seat => {
                      const type = seatTypePresentation(seat.seatTypeCode);
                      return <span key={seat.seatTypeCode} className="flex items-center gap-2 rounded-lg bg-zinc-950 px-2.5 py-2"><span className={`h-4 rounded border ${type.wide ? 'w-7' : 'w-4'} ${type.className}`} /> {seat.seatTypeName || type.label}</span>;
                    })}
                    <span className="mx-1 hidden w-px self-stretch bg-zinc-800 sm:block" />
                    <span className="self-center px-1 font-black uppercase tracking-wider text-zinc-500">Trạng thái</span>
                    <span className="flex items-center gap-2 rounded-lg bg-zinc-950 px-2.5 py-2"><span className="h-4 w-4 rounded border-2 border-brand-orange bg-brand-orange" /> Đang chọn</span>
                    <span className="flex items-center gap-2 rounded-lg bg-zinc-950 px-2.5 py-2"><span className="h-4 w-4 rounded border border-amber-500 bg-amber-500/20" /> Đã khóa vận hành</span>
                    <span className="flex items-center gap-2 rounded-lg bg-zinc-950 px-2.5 py-2"><span className="h-4 w-4 rounded border border-fuchsia-500 bg-fuchsia-950" /> Khách đang giữ</span>
                    <span className="flex items-center gap-2 rounded-lg bg-zinc-950 px-2.5 py-2"><span className="h-4 w-4 rounded border border-zinc-700 bg-zinc-800 opacity-60" /> Đã bán / không dùng được</span>
                  </div>

                  {!availability && layout.showtimeStatus === 'OPEN_FOR_BOOKING' && <div className="flex items-center gap-2 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3 text-xs text-amber-100"><AlertTriangle className="h-4 w-4 shrink-0 text-amber-300" /> Chưa tải được dữ liệu ghế khách đang giữ. Hệ thống vẫn kiểm tra lại trước khi khóa.</div>}

                  <section className="overflow-x-auto rounded-3xl border border-zinc-800 bg-zinc-900/30 p-4 md:p-6">
                    <div className="mx-auto min-w-[680px]">
                      <div className="mx-auto mb-10 max-w-3xl"><div className="h-2 rounded-[100%] bg-gradient-to-r from-transparent via-brand-orange to-transparent shadow-[0_8px_28px_rgba(255,122,0,0.28)]" /><p className="mt-3 text-center text-[10px] font-black tracking-[.35em] text-zinc-500">MÀN HÌNH CHIẾU</p></div>
                      <div className="space-y-3">
                        {rows.map(([rowLabel, units]) => (
                          <div key={rowLabel} className="flex items-center gap-3">
                            <span className="w-7 text-center text-xs font-black text-zinc-500">{rowLabel}</span>
                            <div className="grid flex-1 gap-2" style={{ gridTemplateColumns: `repeat(${maxColumn}, minmax(38px, 1fr))` }}>
                              {units.map(seatUnit => {
                                const occupied = seatUnit.seats
                                  .map(seat => occupiedBySeat.get(seat.publicId))
                                  .find(item => item?.status === 'BOOKED')
                                  || seatUnit.seats
                                    .map(seat => occupiedBySeat.get(seat.publicId))
                                    .find(item => item?.status === 'HELD');
                                const selected = seatUnit.seats.every(seat => selectedIds.includes(seat.publicId));
                                const column = Number(seatUnit.positionColumn ?? seatUnit.seatNumber ?? 0) + 1;
                                const seat = {
                                  ...seatUnit,
                                  seatTypeCode: seatUnit.seatType,
                                  operationalStatus: seatUnit.seats.every(item => item.operationalStatus === 'ACTIVE')
                                    ? 'ACTIVE'
                                    : seatUnit.seats.find(item => item.operationalStatus !== 'ACTIVE')?.operationalStatus,
                                  blocked: seatUnit.seats.some(item => item.blocked),
                                  blockReason: seatUnit.seats.find(item => item.blocked)?.blockReason,
                                };
                                const type = seatTypePresentation(seat.seatTypeCode);
                                const detail = occupied?.status === 'BOOKED'
                                  ? 'Ghế đã bán'
                                  : occupied?.status === 'HELD'
                                    ? 'Khách đang giữ ghế'
                                    : seat.blocked
                                      ? `Đã khóa: ${seat.blockReason || 'Không ghi lý do'}`
                                      : seat.operationalStatus !== 'ACTIVE'
                                      ? 'Ghế đang bảo trì hoặc đã ngưng hoạt động'
                                      : 'Có thể thao tác';
                                const label = `${seat.isCouple ? 'Ghế đôi' : 'Ghế'} ${seat.seatCode} · ${seat.seatTypeName || type.label} · ${detail}`;
                                return <button key={seat.key} type="button" disabled={!canSelectUnit(seatUnit)} onClick={() => toggleSeat(seatUnit)} aria-pressed={selected} aria-label={label} title={label} style={{ gridColumnStart: column, gridColumnEnd: `span ${seat.columnSpan || 1}` }} className={`relative h-10 border px-1 text-[10px] font-black transition-all ${type.wide ? 'rounded-xl border-2' : 'rounded-t-lg rounded-b-xl'} ${seatTone({ seat, occupied, selected, mode })}`}><span>{seat.seatCode}</span>{occupied?.status === 'HELD' && <Clock3 className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-fuchsia-950 p-0.5" />}{seat.blocked && <LockKeyhole className="absolute -right-1 -top-1 h-3 w-3 rounded-full bg-zinc-950 p-0.5 text-amber-300" />}</button>;
                              })}
                            </div>
                            <span className="w-7 text-center text-xs font-black text-zinc-500">{rowLabel}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </section>
                </div>

                <aside className="h-fit space-y-4 xl:sticky xl:top-0">
                  <div className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5">
                    <div className="flex items-center justify-between"><h3 className="text-sm font-black">Ghế đã chọn</h3><span className="rounded-full bg-brand-orange/15 px-2.5 py-1 text-xs font-black text-brand-orange">{selectedSeats.length}</span></div>
                    <div className="mt-3 flex min-h-10 flex-wrap gap-2">
                      {selectedSeats.length ? selectedSeats.map(seat => <span key={seat.publicId} className="rounded-lg border border-zinc-700 bg-zinc-950 px-2.5 py-1.5 text-xs font-black">{seat.seatCode}</span>) : <p className="text-xs leading-5 text-zinc-500">Chọn ghế trực tiếp trên sơ đồ. Ghế đôi sẽ được chọn cùng nhau.</p>}
                    </div>
                  </div>

                  {mode === 'block' && <div className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5"><label className="text-sm font-black">Vì sao cần khóa ghế?</label><p className="mt-1 text-xs leading-5 text-zinc-500">Lý do giúp ca sau hiểu tình trạng và biết khi nào có thể mở lại ghế.</p><div className="mt-3 grid gap-2">{REASON_PRESETS.map(item => <button key={item} type="button" onClick={() => setReason(item)} className={`rounded-xl border px-3 py-2 text-left text-xs font-bold ${reason === item ? 'border-brand-orange bg-brand-orange/10 text-orange-100' : 'border-zinc-800 text-zinc-400 hover:border-zinc-700'}`}>{item}</button>)}</div><textarea value={reason} maxLength={255} onChange={event => setReason(event.target.value)} rows={3} className="mt-3 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 py-2 text-xs text-white outline-none focus:border-brand-orange" placeholder="Hoặc nhập lý do cụ thể…" /></div>}

                  <div className="rounded-2xl border border-blue-500/20 bg-blue-500/5 p-4"><div className="flex gap-3"><Armchair className="h-5 w-5 shrink-0 text-blue-300" /><p className="text-xs leading-5 text-blue-100/75">{mode === 'block' ? 'Khách sẽ không thể chọn ghế bị khóa trong riêng suất này. Các suất chiếu khác không bị ảnh hưởng.' : 'Sau khi mở lại, ghế sẽ xuất hiện cho khách nếu chưa có người khác giữ hoặc mua.'}</p></div></div>
                </aside>
              </section>
            </div>

            <footer className="shrink-0 border-t border-zinc-800 bg-zinc-950/95 px-5 py-4 backdrop-blur md:px-7">
              {state.error && <div role="alert" className="mb-3 flex items-start gap-2 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-xs text-red-100"><ShieldAlert className="h-4 w-4 shrink-0" /> {state.error}</div>}
              {state.success && <div role="status" className="mb-3 flex items-start gap-2 rounded-xl border border-emerald-500/25 bg-emerald-500/10 p-3 text-xs text-emerald-100"><CheckCircle2 className="h-4 w-4 shrink-0" /> {state.success}</div>}
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <button type="button" onClick={load} disabled={state.saving} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 text-xs font-black text-zinc-300 disabled:opacity-40"><RefreshCw className="h-4 w-4" /> Làm mới tình trạng ghế</button>
                <button type="button" disabled={!layout.editable || !selectedIds.length || state.saving} onClick={submit} className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 text-sm font-black disabled:cursor-not-allowed disabled:opacity-40 ${mode === 'block' ? 'bg-amber-500 text-zinc-950' : 'bg-emerald-500 text-zinc-950'}`}>{state.saving ? <Loader2 className="h-4 w-4 animate-spin" /> : mode === 'block' ? <LockKeyhole className="h-4 w-4" /> : <UnlockKeyhole className="h-4 w-4" />}{state.saving ? 'Đang cập nhật…' : mode === 'block' ? `Xác nhận khóa ${selectedIds.length} ghế` : `Mở lại ${selectedIds.length} ghế`}</button>
              </div>
            </footer>
          </>
        )}
      </aside>
    </div>
  );
}
