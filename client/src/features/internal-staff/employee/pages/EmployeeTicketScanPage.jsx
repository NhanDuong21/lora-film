import { useCallback, useEffect, useRef, useState } from 'react';
import {
  Camera, CameraOff, CheckCircle2, CircleAlert, Keyboard,
  LoaderCircle, MapPin, QrCode, RotateCcw, ShieldCheck, Ticket, XCircle,
} from 'lucide-react';
import { getMyEmployeeCinemaContext } from '../services/employeeBoxOfficeService';
import { getTicketScanHistory, scanTicket } from '../services/employeeTicketCheckerService';
import { auditoriumLabel, clock, dateTime, ticketScanResult } from '../employeePresentation';

const resultClasses = {
  emerald: 'border-emerald-500/40 bg-emerald-500/10 text-emerald-200',
  amber: 'border-amber-500/40 bg-amber-500/10 text-amber-100',
  red: 'border-red-500/40 bg-red-500/10 text-red-100',
  zinc: 'border-zinc-700 bg-zinc-900 text-zinc-200',
};

const resultIcon = result => result === 'ADMITTED'
  ? <CheckCircle2 size={52} />
  : result === 'TOO_EARLY' || result === 'NOT_PAID'
    ? <CircleAlert size={52} />
    : <XCircle size={52} />;

export default function EmployeeTicketScanPage() {
  const [context, setContext] = useState(null);
  const [code, setCode] = useState('');
  const [gateLabel, setGateLabel] = useState('Cửa phòng 01');
  const [result, setResult] = useState(null);
  const [recent, setRecent] = useState([]);
  const [state, setState] = useState({ submitting: false, camera: false, error: '', cameraError: '' });
  const inputRef = useRef(null);
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const frameRef = useRef(null);
  const detectorRef = useRef(null);

  const loadRecent = useCallback(async () => {
    try {
      const rows = await getTicketScanHistory({});
      setRecent(rows.slice(0, 5));
    } catch {
      setRecent([]);
    }
  }, []);

  useEffect(() => {
    Promise.allSettled([getMyEmployeeCinemaContext(), loadRecent()]).then(([employee]) => {
      if (employee.status === 'fulfilled') setContext(employee.value);
    });
  }, [loadRecent]);

  const stopCamera = useCallback(() => {
    if (frameRef.current) cancelAnimationFrame(frameRef.current);
    frameRef.current = null;
    streamRef.current?.getTracks?.().forEach(track => track.stop());
    streamRef.current = null;
    detectorRef.current = null;
    if (videoRef.current) videoRef.current.srcObject = null;
    setState(value => ({ ...value, camera: false }));
  }, []);

  useEffect(() => () => {
    if (frameRef.current) cancelAnimationFrame(frameRef.current);
    streamRef.current?.getTracks?.().forEach(track => track.stop());
  }, []);

  const submitTicket = useCallback(async rawCode => {
    const normalized = String(rawCode || '').trim();
    if (!normalized) {
      setState(value => ({ ...value, error: 'Vui lòng quét QR hoặc nhập mã vé trước khi kiểm tra.' }));
      inputRef.current?.focus();
      return;
    }
    setState(value => ({ ...value, submitting: true, error: '' }));
    try {
      const response = await scanTicket({ code: normalized, gateLabel: gateLabel.trim() || 'Cửa soát vé' });
      setResult(response);
      setCode('');
      await loadRecent();
    } catch (error) {
      setResult(null);
      setState(value => ({
        ...value,
        error: error?.response?.data?.message || 'Không thể kiểm tra vé lúc này. Vui lòng thử lại.',
      }));
    } finally {
      setState(value => ({ ...value, submitting: false }));
      window.setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [gateLabel, loadRecent]);

  const startCamera = async () => {
    setState(value => ({ ...value, cameraError: '' }));
    if (!navigator.mediaDevices?.getUserMedia || !window.BarcodeDetector) {
      setState(value => ({
        ...value,
        cameraError: 'Thiết bị này chưa hỗ trợ quét QR trực tiếp. Bạn vẫn có thể dùng máy quét cầm tay hoặc nhập mã vé.',
      }));
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
      streamRef.current = stream;
      detectorRef.current = new window.BarcodeDetector({ formats: ['qr_code'] });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setState(value => ({ ...value, camera: true }));
      const detect = async () => {
        if (!detectorRef.current || !videoRef.current) return;
        try {
          const codes = await detectorRef.current.detect(videoRef.current);
          if (codes[0]?.rawValue) {
            const scannedCode = codes[0].rawValue;
            stopCamera();
            await submitTicket(scannedCode);
            return;
          }
        } catch {
          // Camera frames can be temporarily unavailable while the stream starts.
        }
        frameRef.current = requestAnimationFrame(detect);
      };
      frameRef.current = requestAnimationFrame(detect);
    } catch {
      stopCamera();
      setState(value => ({ ...value, cameraError: 'Không mở được camera. Hãy cấp quyền camera hoặc dùng ô nhập mã bên dưới.' }));
    }
  };

  const presentation = result ? ticketScanResult(result.result) : null;

  return (
    <section className="mx-auto w-full max-w-7xl space-y-6 text-white">
      <header className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-6 md:p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div><p className="text-xs font-black uppercase tracking-[0.22em] text-amber-500">Kiểm soát lối vào</p><h1 className="mt-2 text-3xl font-black">Soát vé</h1><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Quét mã, kiểm tra kết quả và chỉ cho khách vào khi màn hình hiển thị màu xanh.</p></div>
          <div className="rounded-2xl border border-emerald-500/25 bg-emerald-500/[0.06] px-4 py-3"><p className="flex items-center gap-2 text-[10px] font-black uppercase text-emerald-400"><MapPin size={14} /> Rạp đang làm việc</p><p className="mt-1 font-black">{context?.cinemaName || 'Đang xác định rạp'}</p></div>
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(340px,0.75fr)]">
        <div className="space-y-6">
          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"><div><h2 className="flex items-center gap-2 font-black"><QrCode className="text-amber-400" /> Quét QR bằng camera</h2><p className="mt-1 text-sm text-zinc-500">Đưa mã QR vào giữa khung hình và giữ thiết bị ổn định.</p></div><button type="button" onClick={state.camera ? stopCamera : startCamera} className={`flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-black ${state.camera ? 'border border-red-500/30 text-red-300' : 'bg-amber-500 text-black'}`}>{state.camera ? <CameraOff size={18} /> : <Camera size={18} />}{state.camera ? 'Tắt camera' : 'Mở camera'}</button></div>
            <div className={`relative mt-5 aspect-video overflow-hidden rounded-2xl border ${state.camera ? 'border-amber-500/40 bg-black' : 'border-dashed border-zinc-700 bg-zinc-950/70'}`}>
              <video ref={videoRef} muted playsInline className={`h-full w-full object-cover ${state.camera ? 'block' : 'hidden'}`} />
              {state.camera ? <div className="pointer-events-none absolute inset-[16%] rounded-3xl border-2 border-amber-400 shadow-[0_0_0_999px_rgba(0,0,0,0.35)]" /> : <div className="absolute inset-0 grid place-items-center text-center"><div><Camera className="mx-auto text-zinc-700" size={44} /><p className="mt-3 font-black text-zinc-400">Camera đang tắt</p><p className="mt-1 text-xs text-zinc-600">Có thể dùng máy quét cầm tay ở ô nhập mã.</p></div></div>}
            </div>
            {state.cameraError ? <p className="mt-3 rounded-xl border border-amber-500/25 bg-amber-500/10 p-3 text-sm text-amber-200">{state.cameraError}</p> : null}
          </article>

          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/70 p-6">
            <h2 className="flex items-center gap-2 font-black"><Keyboard className="text-sky-400" /> Nhập mã hoặc dùng máy quét cầm tay</h2>
            <form className="mt-5 grid gap-4 md:grid-cols-[1fr_220px_auto]" onSubmit={event => { event.preventDefault(); submitTicket(code); }}>
              <label className="space-y-2"><span className="text-xs font-bold text-zinc-400">Mã QR, mã vé hoặc mã vạch</span><input ref={inputRef} autoFocus value={code} onChange={event => setCode(event.target.value)} placeholder="Quét mã rồi nhấn Enter" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 font-mono text-sm outline-none focus:border-amber-500" /></label>
              <label className="space-y-2"><span className="text-xs font-bold text-zinc-400">Cửa đang trực</span><input value={gateLabel} onChange={event => setGateLabel(event.target.value)} placeholder="Ví dụ: Cửa phòng 01" className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-4 py-3 text-sm outline-none focus:border-amber-500" /></label>
              <button type="submit" disabled={state.submitting} className="mt-auto flex items-center justify-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-black text-black disabled:opacity-50">{state.submitting ? <LoaderCircle className="animate-spin" size={18} /> : <ShieldCheck size={18} />} Kiểm tra vé</button>
            </form>
            {state.error ? <p className="mt-4 rounded-xl border border-red-500/25 bg-red-500/10 p-3 text-sm text-red-200">{state.error}</p> : null}
          </article>
        </div>

        <div className="space-y-6">
          <article className={`min-h-[360px] rounded-3xl border p-6 ${result ? resultClasses[presentation.tone] : resultClasses.zinc}`}>
            {result ? <div className="flex h-full flex-col">
              <div className="flex items-start justify-between gap-4"><div>{resultIcon(result.result)}<p className="mt-4 text-xs font-black uppercase tracking-[0.18em] opacity-70">Kết quả kiểm tra</p><h2 className="mt-1 text-2xl font-black">{presentation.label}</h2></div><button type="button" onClick={() => { setResult(null); inputRef.current?.focus(); }} className="rounded-xl border border-current/20 p-2" aria-label="Quét vé tiếp theo"><RotateCcw size={18} /></button></div>
              <p className="mt-4 text-sm leading-6 opacity-80">{result.message}</p>
              {result.ticketCode ? <div className="mt-6 space-y-3 rounded-2xl bg-black/20 p-4 text-sm"><div className="flex justify-between gap-4"><span className="opacity-60">Phim</span><strong className="text-right">{result.movieTitle || 'Chưa ghi nhận'}</strong></div><div className="flex justify-between gap-4"><span className="opacity-60">Phòng / ghế</span><strong>{auditoriumLabel(result.auditoriumName)} · {result.seatLabel}</strong></div><div className="flex justify-between gap-4"><span className="opacity-60">Giờ chiếu</span><strong>{clock(result.showtimeStart)}</strong></div><div className="flex justify-between gap-4"><span className="opacity-60">Mã vé</span><strong className="break-all text-right font-mono text-xs">{result.ticketCode}</strong></div>{result.usedAt ? <div className="flex justify-between gap-4"><span className="opacity-60">Ghi nhận vào</span><strong>{dateTime(result.usedAt)}</strong></div> : null}</div> : null}
              <button type="button" onClick={() => { setResult(null); inputRef.current?.focus(); }} className="mt-6 w-full rounded-xl bg-white py-3 text-sm font-black text-black">Soát vé tiếp theo</button>
            </div> : <div className="grid min-h-[310px] place-items-center text-center"><div><Ticket className="mx-auto text-zinc-700" size={48} /><h2 className="mt-4 text-lg font-black text-zinc-400">Chờ quét vé</h2><p className="mt-2 max-w-sm text-sm leading-6 text-zinc-600">Kết quả sẽ hiện thật rõ tại đây. Chỉ cho khách vào khi nhận được thông báo vé hợp lệ màu xanh.</p></div></div>}
          </article>

          <article className="rounded-3xl border border-zinc-800 bg-zinc-900/60 p-5"><h2 className="font-black">Lượt quét gần nhất</h2>{recent.length ? <div className="mt-4 space-y-2">{recent.map(item => { const itemPresentation = ticketScanResult(item.result); return <div key={item.eventPublicId} className="flex items-center justify-between gap-3 rounded-xl bg-zinc-950/70 p-3"><div className="min-w-0"><p className="truncate text-sm font-bold">{item.seatLabel ? `Ghế ${item.seatLabel} · ${item.movieTitle}` : item.message}</p><p className="mt-1 text-[11px] text-zinc-600">{dateTime(item.scannedAt)}</p></div><span className={`shrink-0 rounded-full px-2.5 py-1 text-[10px] font-black ${itemPresentation.tone === 'emerald' ? 'bg-emerald-500/10 text-emerald-300' : itemPresentation.tone === 'amber' ? 'bg-amber-500/10 text-amber-300' : 'bg-red-500/10 text-red-300'}`}>{itemPresentation.shortLabel}</span></div>; })}</div> : <p className="py-8 text-center text-sm text-zinc-600">Chưa có lượt quét nào trong ngày.</p>}</article>
        </div>
      </div>
    </section>
  );
}
