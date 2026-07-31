import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, CheckCircle2, Eye, Info, Plus, Save, Trash2 } from 'lucide-react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import adminPricingService from '../services/adminPricingService';
import { cinemaLocalDateTimeToInstant } from '../utils/pricingDateTime';
import { getConflictPresentation, getPricingReasonPresentation } from '../utils/pricingPresentation';

const emptyRule = (seatTypeId = '') => ({
  scope: 'CINEMA',
  seatTypeId,
  auditoriumId: '',
  screenType: '',
  dayType: 'ALL_DAYS',
  timeBandStart: '',
  timeBandEnd: '',
  price: '',
  active: true,
});

const inputClassName = 'min-h-11 w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-brand-orange focus:ring-2 focus:ring-brand-orange/20';
const money = value => value ? `${new Intl.NumberFormat('vi-VN').format(Number(value))} ₫` : 'Chưa nhập';

export default function AdminPricingPolicyFormPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const navigate = useNavigate();
  const { triggerToast } = useOutletContext() || {};
  const [cinemas, setCinemas] = useState([]);
  const [seatTypes, setSeatTypes] = useState([]);
  const [auditoriums, setAuditoriums] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [conflicts, setConflicts] = useState([]);
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [previewInput, setPreviewInput] = useState({ auditoriumId: '', startTime: '' });
  const [preview, setPreview] = useState(null);
  const [previewing, setPreviewing] = useState(false);
  const [form, setForm] = useState({
    name: '',
    cinemaId: '',
    effectiveFrom: new Date().toISOString().slice(0, 10),
    effectiveTo: '',
    currency: 'VND',
    priority: 0,
    rules: [emptyRule()],
  });

  useEffect(() => {
    Promise.all([
      adminCinemaService.getCinemas({ page: 0, size: 100 }),
      adminRoomService.getSeatTypes(),
      editing ? adminPricingService.getPolicy(id) : Promise.resolve(null),
    ]).then(([cinemaResponse, seatResponse, policyResponse]) => {
      const activeSeats = (seatResponse?.data || []).filter(item => item.status === 'ACTIVE');
      setCinemas(cinemaResponse?.data?.data || []);
      setSeatTypes(activeSeats);
      const policy = policyResponse?.data;
      if (policy) {
        if (policy.storedStatus !== 'DRAFT') {
          triggerToast?.('Chỉ bảng giá đang soạn mới có thể chỉnh sửa.', 'error');
          navigate(`/admin/pricing/${id}`, { replace: true });
          return;
        }
        setForm({
          name: policy.name,
          cinemaId: policy.cinemaId,
          effectiveFrom: policy.effectiveFrom,
          effectiveTo: policy.effectiveTo || '',
          currency: policy.currency,
          priority: policy.priority,
          expectedVersion: policy.version,
          rules: policy.rules.map(rule => ({
            scope: rule.auditoriumId ? 'AUDITORIUM' : rule.screenType ? 'SCREEN_TYPE' : 'CINEMA',
            seatTypeId: rule.seatTypeId,
            auditoriumId: rule.auditoriumId || '',
            screenType: rule.screenType || '',
            dayType: rule.dayType,
            timeBandStart: rule.timeBandStart || '',
            timeBandEnd: rule.timeBandEnd || '',
            price: rule.price,
            active: rule.active,
          })),
        });
      } else if (activeSeats.length > 0) {
        setForm(current => ({ ...current, rules: activeSeats.map(seat => emptyRule(seat.publicId)) }));
      }
    }).catch(error => triggerToast?.(error.response?.data?.message || 'Không thể tải biểu mẫu giá.', 'error'));
  }, [editing, id, navigate, triggerToast]);

  useEffect(() => {
    if (!form.cinemaId) {
      return;
    }
    adminCinemaService.getAdminCinemaDetail(form.cinemaId)
      .then(response => setAuditoriums(response?.data?.auditoriums || response?.data?.activeAuditoriums || []))
      .catch(() => setAuditoriums([]));
  }, [form.cinemaId]);

  const canSubmit = useMemo(() => Boolean(
    form.name.trim()
      && form.cinemaId
      && form.effectiveFrom
      && form.rules.length > 0
      && form.rules.every(rule => rule.seatTypeId && Number(rule.price) > 0
        && (rule.scope !== 'SCREEN_TYPE' || rule.screenType)
        && (rule.scope !== 'AUDITORIUM' || rule.auditoriumId)),
  ), [form]);

  const setRule = (index, field, value) => {
    setForm(current => ({
      ...current,
      rules: current.rules.map((rule, ruleIndex) => {
        if (ruleIndex !== index) return rule;
        const next = { ...rule, [field]: value };
        if (field === 'scope') {
          next.auditoriumId = '';
          next.screenType = '';
        }
        if (field === 'auditoriumId' && value) next.screenType = '';
        if (field === 'screenType' && value) next.auditoriumId = '';
        return next;
      }),
    }));
  };

  const chooseCinema = value => {
    setAuditoriums([]);
    setPreview(null);
    setPreviewInput(current => ({ ...current, auditoriumId: '' }));
    setForm(current => ({
      ...current,
      cinemaId: value,
      rules: current.rules.map(rule => rule.scope === 'AUDITORIUM' ? { ...rule, auditoriumId: '' } : rule),
    }));
  };

  const submit = async event => {
    event.preventDefault();
    if (!canSubmit) return;
    setSubmitting(true);
    const payload = {
      ...form,
      effectiveTo: form.effectiveTo || null,
      priority: Number(form.priority),
      rules: form.rules.map(rule => ({
        ...rule,
        scope: undefined,
        auditoriumId: rule.scope === 'AUDITORIUM' ? rule.auditoriumId : null,
        screenType: rule.scope === 'SCREEN_TYPE' ? rule.screenType : null,
        timeBandStart: rule.timeBandStart || null,
        timeBandEnd: rule.timeBandEnd || null,
        price: Number(rule.price),
      })),
    };
    try {
      const response = editing ? await adminPricingService.updatePolicy(id, payload) : await adminPricingService.createPolicy(payload);
      setConflicts(response?.data?.conflicts || []);
      triggerToast?.('Đã lưu bảng giá đang soạn.', 'success');
      navigate(`/admin/pricing/${response.data.publicId}`);
    } catch (error) {
      if (error?.errorCode === 'PRICE_POLICY_OVERLAP' && Array.isArray(error?.data)) setConflicts(error.data);
      triggerToast?.(
        (error?.errorCode?.startsWith('PRICE_') ? getPricingReasonPresentation(error.errorCode).label : null)
          || error?.message
          || 'Không thể lưu bảng giá.',
        'error',
      );
    } finally {
      setSubmitting(false);
    }
  };

  const runPreview = async () => {
    if (!form.cinemaId || !previewInput.auditoriumId || !previewInput.startTime) return;
    setPreviewing(true);
    try {
      const cinema = cinemas.find(item => item.publicId === form.cinemaId);
      const response = await adminPricingService.previewResolution({
        cinemaId: form.cinemaId,
        auditoriumId: previewInput.auditoriumId,
        startTime: cinemaLocalDateTimeToInstant(previewInput.startTime, cinema?.timezone),
      });
      setPreview(response.data);
    } catch (error) {
      setPreview(null);
      triggerToast?.(error.response?.data?.message || 'Không thể xem trước giá.', 'error');
    } finally {
      setPreviewing(false);
    }
  };

  return (
    <form onSubmit={submit} className="min-h-full space-y-6 bg-zinc-950 text-white">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-6 md:flex-row md:items-end md:justify-between">
        <div className="flex items-start gap-3">
          <button type="button" onClick={() => navigate(-1)} aria-label="Quay lại" className="mt-1 rounded-xl p-2 text-zinc-400 hover:bg-zinc-800 hover:text-white"><ArrowLeft className="h-5 w-5" aria-hidden="true" /></button>
          <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-brand-orange">Bảng giá</p><h1 className="mt-2 text-3xl font-black">{editing ? 'Chỉnh sửa bảng giá' : 'Tạo bảng giá'}</h1><p className="mt-2 text-sm text-zinc-400">Nhập giá cơ bản theo loại ghế. Quy tắc đặc biệt có thể thêm sau.</p></div>
        </div>
        <button type="submit" disabled={!canSubmit || submitting} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950 disabled:opacity-40"><Save className="h-4 w-4" aria-hidden="true" /> {submitting ? 'Đang lưu…' : 'Lưu bảng giá'}</button>
      </header>

      <section className="rounded-2xl border border-blue-500/25 bg-blue-500/10 p-4 text-sm text-blue-100">
        <div className="flex items-start gap-3"><Info className="mt-0.5 h-5 w-5 shrink-0 text-blue-300" aria-hidden="true" /><p>Bảng giá đang soạn chưa ảnh hưởng đến khách. Sau khi kiểm tra, bạn có thể kích hoạt cho các suất chiếu mới.</p></div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6">
        <h2 className="text-xl font-black">Thông tin chung</h2>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <label className="space-y-1.5 text-sm font-bold text-zinc-300 md:col-span-2">Tên bảng giá<input required value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} placeholder="Ví dụ: Giá tiêu chuẩn 2026 – Vincom" className={inputClassName} /></label>
          <label className="space-y-1.5 text-sm font-bold text-zinc-300">Áp dụng tại rạp<select required aria-label="Rạp" value={form.cinemaId} onChange={event => chooseCinema(event.target.value)} className={inputClassName}><option value="">Chọn rạp</option>{cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}</select></label>
          <label className="space-y-1.5 text-sm font-bold text-zinc-300">Bắt đầu áp dụng<input type="date" required value={form.effectiveFrom} onChange={event => setForm(current => ({ ...current, effectiveFrom: event.target.value }))} className={inputClassName} /></label>
          <label className="space-y-1.5 text-sm font-bold text-zinc-300">Kết thúc áp dụng <span className="font-normal text-zinc-500">(không bắt buộc)</span><input type="date" value={form.effectiveTo} onChange={event => setForm(current => ({ ...current, effectiveTo: event.target.value }))} className={inputClassName} /></label>
        </div>
      </section>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-5 md:p-6">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between"><div><h2 className="text-xl font-black">Giá vé theo loại ghế</h2><p className="mt-1 text-sm text-zinc-500">Giá cơ bản áp dụng cho mọi ngày và mọi phòng trong rạp.</p></div><button type="button" onClick={() => setForm(current => ({ ...current, rules: [...current.rules, emptyRule()] }))} className="inline-flex min-h-10 items-center gap-2 self-start rounded-xl border border-zinc-700 px-3 text-sm font-bold text-zinc-300 hover:bg-zinc-800"><Plus className="h-4 w-4" aria-hidden="true" /> Thêm loại ghế</button></div>
        <div className="mt-5 space-y-3">
          {form.rules.map((rule, index) => {
            const seat = seatTypes.find(item => item.publicId === rule.seatTypeId);
            return (
              <div key={index} className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
                <div className="grid gap-3 md:grid-cols-[1fr_220px_auto] md:items-end">
                  <label className="space-y-1.5 text-sm font-bold text-zinc-300">Loại ghế<select required value={rule.seatTypeId} onChange={event => setRule(index, 'seatTypeId', event.target.value)} className={inputClassName}><option value="">Chọn loại ghế</option>{seatTypes.map(item => <option key={item.publicId} value={item.publicId}>{item.name} ({item.code})</option>)}</select></label>
                  <label className="space-y-1.5 text-sm font-bold text-zinc-300">Giá vé<input type="number" min="1" required value={rule.price} onChange={event => setRule(index, 'price', event.target.value)} placeholder="Ví dụ: 68000" className={inputClassName} /><span className="mt-1 block text-xs font-normal text-zinc-500">{money(rule.price)}</span></label>
                  <button type="button" disabled={form.rules.length === 1} onClick={() => setForm(current => ({ ...current, rules: current.rules.filter((_, ruleIndex) => ruleIndex !== index) }))} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-500/30 px-3 text-sm font-bold text-rose-300 disabled:cursor-not-allowed disabled:opacity-30"><Trash2 className="h-4 w-4" aria-hidden="true" /> Xóa</button>
                </div>
                {seat && <p className="mt-3 text-xs text-zinc-500">Giá này áp dụng cho mọi phòng, mọi ngày. Muốn tạo giá cuối tuần hoặc giá IMAX, mở “Tùy chọn nâng cao”.</p>}
              </div>
            );
          })}
        </div>
      </section>

      <details open={advancedOpen} onToggle={event => setAdvancedOpen(event.currentTarget.open)} className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5 md:p-6">
        <summary className="cursor-pointer text-base font-black text-zinc-200">Tùy chọn nâng cao <span className="ml-2 text-xs font-normal text-zinc-500">Giá cuối tuần, IMAX, từng phòng hoặc khung giờ</span></summary>
        <div className="mt-5 space-y-3 border-t border-zinc-800 pt-5">
          {form.rules.map((rule, index) => (
            <div key={`advanced-${index}`} className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-950 p-4 md:grid-cols-4">
              <select aria-label={`Phạm vi quy tắc ${index + 1}`} value={rule.scope} onChange={event => setRule(index, 'scope', event.target.value)} className={inputClassName}><option value="CINEMA">Mọi phòng</option><option value="SCREEN_TYPE">Theo loại màn hình</option><option value="AUDITORIUM">Theo phòng cụ thể</option></select>
              {rule.scope === 'AUDITORIUM' ? <div><select aria-label={`Phòng chiếu quy tắc ${index + 1}`} required value={rule.auditoriumId} onChange={event => setRule(index, 'auditoriumId', event.target.value)} className={inputClassName}><option value="">Chọn phòng chiếu</option>{auditoriums.map(item => <option key={item.publicId} value={item.publicId}>{item.name}</option>)}</select>{rule.auditoriumId && <p className="mt-1 px-1 text-[10px] text-zinc-500">Loại màn hình: {auditoriums.find(item => item.publicId === rule.auditoriumId)?.screenType || 'Chưa xác định'}</p>}</div> : rule.scope === 'SCREEN_TYPE' ? <select aria-label={`Loại màn hình quy tắc ${index + 1}`} required value={rule.screenType} onChange={event => setRule(index, 'screenType', event.target.value)} className={inputClassName}><option value="">Chọn loại màn hình</option><option value="STANDARD">STANDARD</option><option value="IMAX">IMAX</option><option value="4DX">4DX</option><option value="SCREENX">SCREENX</option></select> : <div className="flex min-h-11 items-center rounded-xl border border-zinc-800 px-3 text-sm text-zinc-500">Áp dụng cho mọi phòng</div>}
              <select value={rule.dayType} onChange={event => setRule(index, 'dayType', event.target.value)} className={inputClassName}><option value="ALL_DAYS">Mọi ngày</option><option value="WEEKDAY">Ngày thường</option><option value="WEEKEND">Cuối tuần</option></select>
              <div className="grid grid-cols-2 gap-2"><input type="time" aria-label={`Giờ bắt đầu quy tắc ${index + 1}`} value={rule.timeBandStart} onChange={event => setRule(index, 'timeBandStart', event.target.value)} className={inputClassName} /><input type="time" aria-label={`Giờ kết thúc quy tắc ${index + 1}`} value={rule.timeBandEnd} onChange={event => setRule(index, 'timeBandEnd', event.target.value)} className={inputClassName} /></div>
            </div>
          ))}
        </div>
      </details>

      <section className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-5 md:p-6">
        <div className="flex items-start gap-3"><Eye className="mt-0.5 h-5 w-5 text-brand-orange" aria-hidden="true" /><div><h2 className="text-xl font-black">Xem thử giá</h2><p className="mt-1 text-sm text-zinc-500">Chọn một phòng và giờ chiếu để biết khách sẽ thấy mức giá nào.</p></div></div>
        <div className="mt-5 grid gap-3 md:grid-cols-[1fr_1fr_auto]">
          <select value={previewInput.auditoriumId} onChange={event => setPreviewInput(current => ({ ...current, auditoriumId: event.target.value }))} className={inputClassName}><option value="">Chọn phòng chiếu</option>{auditoriums.map(item => <option key={item.publicId} value={item.publicId}>{item.name}</option>)}</select>
          <input type="datetime-local" value={previewInput.startTime} onChange={event => setPreviewInput(current => ({ ...current, startTime: event.target.value }))} className={inputClassName} />
          <button type="button" disabled={previewing || !form.cinemaId || !previewInput.auditoriumId || !previewInput.startTime} onClick={runPreview} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-brand-orange/40 px-4 text-sm font-black text-brand-orange disabled:opacity-40">{previewing ? 'Đang xem…' : 'Xem thử'}</button>
        </div>
        {preview && <div className={`mt-4 rounded-xl border p-4 ${preview.complete ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-rose-500/30 bg-rose-500/10'}`}><p className={`flex items-center gap-2 font-black ${preview.complete ? 'text-emerald-300' : 'text-rose-300'}`}>{preview.complete ? <CheckCircle2 className="h-4 w-4" aria-hidden="true" /> : <Info className="h-4 w-4" aria-hidden="true" />}{preview.complete ? 'Đủ giá để bán' : 'Chưa đủ giá để bán'}</p><div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">{preview.prices?.map(line => <div key={line.seatTypeId} className="rounded-xl bg-zinc-950/60 p-3 text-xs"><p className="font-bold text-zinc-200">{line.seatTypeName}</p><p className="mt-1 text-base font-black text-emerald-300">{money(line.price)}</p></div>)}</div>{preview.missingSeatTypes?.length > 0 && <p className="mt-3 text-xs text-rose-200">{getPricingReasonPresentation('PRICING_INCOMPLETE').label}: {preview.missingSeatTypes.map(item => item.seatTypeName).join(', ')}</p>}</div>}
      </section>

      {conflicts.length > 0 && <section className="rounded-2xl border border-rose-500/30 bg-rose-500/10 p-5"><h2 className="font-black text-rose-300">Có mức giá bị trùng</h2>{conflicts.map(conflict => { const presentation = getConflictPresentation(conflict); return <div key={`${conflict.firstRuleId}-${conflict.secondRuleId}`} className="mt-3 rounded-xl border border-rose-500/20 bg-zinc-950/30 p-3 text-sm text-rose-100"><p className="font-black">{presentation.title}</p><p className="mt-1">{presentation.facts}</p><p className="mt-2 text-rose-200">{presentation.guidance}</p></div>; })}</section>}
    </form>
  );
}
