import { useCallback, useEffect, useRef, useState } from 'react';
import { BrowserQRCodeReader } from '@zxing/browser';
import {
  Camera, CameraOff, CheckCircle2, CircleAlert, Keyboard,
  LoaderCircle, MapPin, QrCode, RotateCcw, ShieldCheck, Ticket, Upload, XCircle,
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

const cameraErrorMessage = error => {
  if (!window.isSecureContext) {
    return 'Camera trực tiếp cần kết nối HTTPS. Trên điện thoại, hãy mở địa chỉ HTTPS của hệ thống hoặc dùng nút “Chụp / chọn ảnh QR”.';
  }
  if (error?.name === 'NotAllowedError') {
    return 'Bạn chưa cấp quyền camera. Hãy chọn Cho phép trong cài đặt trình duyệt rồi mở camera lại.';
  }
  if (error?.name === 'NotFoundError') {
    return 'Không tìm thấy camera trên thiết bị này. Bạn có thể chụp hoặc chọn ảnh QR để kiểm tra vé.';
  }
  if (error?.name === 'NotReadableError') {
    return 'Camera đang được ứng dụng khác sử dụng. Hãy đóng ứng dụng đó rồi thử lại.';
  }
  return 'Không mở được camera. Hãy kiểm tra quyền camera hoặc dùng nút “Chụp / chọn ảnh QR”.';
};

export default function EmployeeTicketScanPage() {
  const [context, setContext] = useState(null);
  const [code, setCode] = useState('');
  const [gateLabel, setGateLabel] = useState('Cửa phòng 01');
  const [result, setResult] = useState(null);
  const [recent, setRecent] = useState([]);
  const [state, setState] = useState({ submitting: false, camera: false, cameraStarting: false, imageDecoding: false, error: '', cameraError: '' });
  const inputRef = useRef(null);
  const videoRef = useRef(null);
  const imageInputRef = useRef(null);
  const scannerControlsRef = useRef(null);
  const scanLockRef = useRef(false);
  const cameraRequestRef = useRef(0);

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
    cameraRequestRef.current += 1;
    scannerControlsRef.current?.stop?.();
    scannerControlsRef.current = null;
    scanLockRef.current = false;
    videoRef.current?.srcObject?.getTracks?.().forEach(track => track.stop());
    if (videoRef.current) videoRef.current.srcObject = null;
    setState(value => ({ ...value, camera: false, cameraStarting: false }));
  }, []);

  useEffect(() => () => {
    scannerControlsRef.current?.stop?.();
    videoRef.current?.srcObject?.getTracks?.().forEach(track => track.stop());
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
    const requestId = cameraRequestRef.current + 1;
    cameraRequestRef.current = requestId;
    setState(value => ({ ...value, cameraStarting: true, cameraError: '' }));
    if (!navigator.mediaDevices?.getUserMedia) {
      setState(value => ({
        ...value,
        cameraStarting: false,
        cameraError: window.isSecureContext
          ? 'Trình duyệt này không cho trang web dùng camera. Hãy mở bằng Chrome hoặc Safari, hoặc dùng nút “Chụp / chọn ảnh QR”.'
          : 'Camera trực tiếp cần kết nối HTTPS. Trên điện thoại, hãy mở địa chỉ HTTPS của hệ thống hoặc dùng nút “Chụp / chọn ảnh QR”.',
      }));
      return;
    }
    try {
      scanLockRef.current = false;
      const reader = new BrowserQRCodeReader(undefined, {
        delayBetweenScanAttempts: 150,
        delayBetweenScanSuccess: 800,
      });
      const controls = await reader.decodeFromConstraints(
        {
          video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1280 },
            height: { ideal: 720 },
          },
          audio: false,
        },
        videoRef.current,
        (...scanArgs) => {
          const [decoded, , activeControls] = scanArgs;
          if (cameraRequestRef.current !== requestId) {
            activeControls?.stop?.();
            return;
          }
          if (!decoded || scanLockRef.current) return;
          scanLockRef.current = true;
          activeControls?.stop?.();
          scannerControlsRef.current = null;
          setState(value => ({ ...value, camera: false, cameraStarting: false, cameraError: '' }));
          void submitTicket(decoded.getText());
        },
      );
      if (cameraRequestRef.current !== requestId) {
        controls.stop();
        return;
      }
      scannerControlsRef.current = controls;
      setState(value => ({ ...value, camera: true, cameraStarting: false }));
    } catch (error) {
      stopCamera();
      setState(value => ({ ...value, cameraError: cameraErrorMessage(error) }));
    }
  };

  const scanQrImage = async event => {
    const [file] = Array.from(event.target.files || []);
    event.target.value = '';
    if (!file) return;

    stopCamera();
    setState(value => ({ ...value, imageDecoding: true, cameraError: '' }));
    const imageUrl = URL.createObjectURL(file);
    try {
      const decoded = await new BrowserQRCodeReader().decodeFromImageUrl(imageUrl);
      await submitTicket(decoded.getText());
    } catch {
      setState(value => ({
        ...value,
        cameraError: 'Không đọc được mã QR trong ảnh. Hãy chụp gần hơn, đủ sáng và giữ trọn mã QR trong khung hình.',
      }));
    } finally {
      URL.revokeObjectURL(imageUrl);
      setState(value => ({ ...value, imageDecoding: false }));
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
            <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
              <div><h2 className="flex items-center gap-2 font-black"><QrCode className="text-amber-400" /> Quét QR bằng camera</h2><p className="mt-1 text-sm text-zinc-500">Đưa mã QR vào giữa khung hình hoặc chụp một ảnh QR rõ nét.</p></div>
              <div className="flex flex-col gap-2 sm:flex-row">
                <button type="button" disabled={state.cameraStarting} onClick={state.camera ? stopCamera : startCamera} className={`flex items-center justify-center gap-2 rounded-xl px-4 py-3 text-sm font-black disabled:opacity-60 ${state.camera ? 'border border-red-500/30 text-red-300' : 'bg-amber-500 text-black'}`}>{state.cameraStarting ? <LoaderCircle className="animate-spin" size={18} /> : state.camera ? <CameraOff size={18} /> : <Camera size={18} />}{state.cameraStarting ? 'Đang xin quyền camera…' : state.camera ? 'Tắt camera' : 'Mở camera trực tiếp'}</button>
                <button type="button" disabled={state.imageDecoding} onClick={() => imageInputRef.current?.click()} className="flex items-center justify-center gap-2 rounded-xl border border-zinc-700 px-4 py-3 text-sm font-black text-zinc-200 disabled:opacity-50">{state.imageDecoding ? <LoaderCircle className="animate-spin" size={18} /> : <Upload size={18} />}{state.imageDecoding ? 'Đang đọc QR…' : 'Chụp / chọn ảnh QR'}</button>
                <input ref={imageInputRef} type="file" accept="image/*" capture="environment" onChange={scanQrImage} className="hidden" aria-label="Chụp hoặc chọn ảnh QR" />
              </div>
            </div>
            <div className={`relative mt-5 aspect-video overflow-hidden rounded-2xl border ${state.camera ? 'border-amber-500/40 bg-black' : 'border-dashed border-zinc-700 bg-zinc-950/70'}`}>
              <video ref={videoRef} muted playsInline className={`h-full w-full object-cover ${state.camera ? 'block' : 'hidden'}`} />
              {state.camera ? <div className="pointer-events-none absolute inset-[16%] rounded-3xl border-2 border-amber-400 shadow-[0_0_0_999px_rgba(0,0,0,0.35)]"><span className="absolute -bottom-10 left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-black/80 px-3 py-1 text-xs font-bold text-white">Đang tìm mã QR…</span></div> : <div className="absolute inset-0 grid place-items-center text-center"><div><Camera className="mx-auto text-zinc-700" size={44} /><p className="mt-3 font-black text-zinc-400">Camera đang tắt</p><p className="mt-1 text-xs text-zinc-600">Trên điện thoại, có thể mở camera trực tiếp hoặc chụp ảnh QR.</p></div></div>}
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
