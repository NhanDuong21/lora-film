import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, Eye, Plus, Save, Trash2 } from 'lucide-react';
import { useNavigate, useOutletContext, useParams } from 'react-router-dom';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import adminRoomService from '@/features/facilities/admin/services/adminRoomService';
import adminPricingService from '../services/adminPricingService';
import { cinemaLocalDateTimeToInstant } from '../utils/pricingDateTime';
import {
  getConflictPresentation,
  getPricingReasonPresentation,
} from '../utils/pricingPresentation';

const emptyRule = () => ({
  scope: 'CINEMA',
  seatTypeId: '',
  auditoriumId: '',
  screenType: '',
  dayType: 'ALL_DAYS',
  timeBandStart: '',
  timeBandEnd: '',
  price: '',
  active: true,
});

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
    expectedVersion: null,
    rules: [emptyRule()],
  });

  useEffect(() => {
    Promise.all([
      adminCinemaService.getCinemas({ page: 0, size: 100 }),
      adminRoomService.getSeatTypes(),
      editing ? adminPricingService.getPolicy(id) : Promise.resolve(null),
    ]).then(([cinemaResponse, seatResponse, policyResponse]) => {
      setCinemas(cinemaResponse?.data?.data || []);
      const activeSeatTypes = (seatResponse?.data || []).filter(item => item.status === 'ACTIVE');
      setSeatTypes(activeSeatTypes);
      const policy = policyResponse?.data;
      if (policy) {
        if (policy.storedStatus !== 'DRAFT') {
          triggerToast?.('Chỉ chính sách DRAFT mới có thể chỉnh sửa', 'error');
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
        setConflicts(policy.conflicts || []);
      } else if (activeSeatTypes.length > 0) {
        setForm(current => ({
          ...current,
          rules: activeSeatTypes.map(seatType => ({
            ...emptyRule(),
            seatTypeId: seatType.publicId,
          })),
        }));
      }
    }).catch(error => triggerToast?.(error.response?.data?.message || 'Không thể tải biểu mẫu giá', 'error'));
  }, [editing, id, navigate, triggerToast]);

  useEffect(() => {
    if (!form.cinemaId) {
      return;
    }
    adminCinemaService.getAdminCinemaDetail(form.cinemaId)
      .then(response => setAuditoriums(response?.data?.auditoriums || []))
      .catch(() => setAuditoriums([]));
  }, [form.cinemaId]);

  const canSubmit = useMemo(() => form.name.trim() && form.cinemaId
    && form.effectiveFrom && form.rules.length > 0
    && form.rules.every(rule => rule.seatTypeId
      && Number(rule.price) > 0
      && (rule.scope !== 'SCREEN_TYPE' || rule.screenType)
      && (rule.scope !== 'AUDITORIUM' || rule.auditoriumId)), [form]);

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
      const response = editing
        ? await adminPricingService.updatePolicy(id, payload)
        : await adminPricingService.createPolicy(payload);
      setConflicts(response?.data?.conflicts || []);
      triggerToast?.('Đã lưu bản nháp chính sách giá', 'success');
      navigate(`/admin/pricing/${response.data.publicId}`);
    } catch (error) {
      if (error?.errorCode === 'PRICE_POLICY_OVERLAP' && Array.isArray(error?.data)) {
        setConflicts(error.data);
      }
      triggerToast?.(
        (error?.errorCode?.startsWith('PRIC') || error?.errorCode?.startsWith('PRICE_')
          ? getPricingReasonPresentation(error.errorCode).label
          : null)
          || error?.message
          || 'Không thể lưu chính sách giá',
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
      triggerToast?.(error.response?.data?.message || 'Không thể xem trước giá', 'error');
    } finally {
      setPreviewing(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-6 bg-zinc-950 text-white">
      <div className="flex items-center justify-between border-b border-zinc-800 pb-5">
        <div className="flex items-center gap-3">
          <button type="button" onClick={() => navigate(-1)} className="rounded-xl p-2 text-zinc-400 hover:bg-zinc-800"><ArrowLeft className="h-5 w-5" /></button>
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-amber-400">Draft policy</p>
            <h1 className="text-2xl font-black">{editing ? 'Sửa chính sách giá' : 'Tạo chính sách giá'}</h1>
          </div>
        </div>
        <button disabled={!canSubmit || submitting} className="flex items-center gap-2 rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-black text-zinc-950 disabled:opacity-40">
          <Save className="h-4 w-4" /> {submitting ? 'Đang lưu…' : 'Lưu bản nháp'}
        </button>
      </div>

      <section className="grid gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5 md:grid-cols-3">
        <label className="space-y-1 text-xs font-bold text-zinc-400 md:col-span-2">Tên chính sách
          <input required value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-zinc-100" />
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">Rạp
          <select required value={form.cinemaId} onChange={event => {
            setAuditoriums([]);
            setPreview(null);
            setPreviewInput(current => ({ ...current, auditoriumId: '' }));
            setForm(current => ({
              ...current,
              cinemaId: event.target.value,
              rules: current.rules.map(rule => (
                rule.scope === 'AUDITORIUM' ? { ...rule, auditoriumId: '' } : rule
              )),
            }));
          }} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-zinc-100">
            <option value="">Chọn rạp</option>
            {cinemas.map(cinema => <option key={cinema.publicId} value={cinema.publicId}>{cinema.name}</option>)}
          </select>
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">Hiệu lực từ
          <input type="date" required value={form.effectiveFrom} onChange={event => setForm(current => ({ ...current, effectiveFrom: event.target.value }))} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100" />
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">Hiệu lực đến
          <input type="date" value={form.effectiveTo} onChange={event => setForm(current => ({ ...current, effectiveTo: event.target.value }))} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100" />
        </label>
        <label className="space-y-1 text-xs font-bold text-zinc-400">Ưu tiên
          <input type="number" value={form.priority} onChange={event => setForm(current => ({ ...current, priority: event.target.value }))} className="w-full rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2 text-zinc-100" />
        </label>
      </section>

      <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-black">Ma trận giá và quy tắc ghi đè</h2>
            <p className="mt-1 text-xs text-zinc-500">Để trống phạm vi, ngày và giờ để tạo mức giá cơ sở toàn rạp.</p>
          </div>
          <button type="button" onClick={() => setForm(current => ({ ...current, rules: [...current.rules, emptyRule()] }))} className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold hover:bg-zinc-800"><Plus className="h-4 w-4" /> Thêm quy tắc</button>
        </div>
        <div className="space-y-3">
          {form.rules.map((rule, index) => (
            <div key={index} className="grid gap-3 rounded-xl border border-zinc-800 bg-zinc-950 p-4 md:grid-cols-4">
              <select required value={rule.seatTypeId} onChange={event => setRule(index, 'seatTypeId', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs">
                <option value="">Loại ghế</option>
                {seatTypes.map(item => <option key={item.publicId} value={item.publicId}>{item.name} ({item.code})</option>)}
              </select>
              <select aria-label={`Phạm vi quy tắc ${index + 1}`} value={rule.scope} onChange={event => setRule(index, 'scope', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs">
                <option value="CINEMA">Toàn rạp</option>
                <option value="SCREEN_TYPE">Theo loại màn hình</option>
                <option value="AUDITORIUM">Phòng chiếu cụ thể</option>
              </select>
              {rule.scope === 'AUDITORIUM' && (
                <div className="space-y-1">
                  <select aria-label={`Phòng chiếu quy tắc ${index + 1}`} required value={rule.auditoriumId} onChange={event => setRule(index, 'auditoriumId', event.target.value)} className="w-full rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs">
                    <option value="">Chọn phòng chiếu</option>
                    {auditoriums.map(item => <option key={item.publicId} value={item.publicId}>{item.name}</option>)}
                  </select>
                  {rule.auditoriumId && (
                    <p className="px-1 text-[10px] text-zinc-500">
                      Loại màn hình: {auditoriums.find(item => item.publicId === rule.auditoriumId)?.screenType || 'Chưa xác định'}
                    </p>
                  )}
                </div>
              )}
              {rule.scope === 'SCREEN_TYPE' && (
                <select aria-label={`Loại màn hình quy tắc ${index + 1}`} required value={rule.screenType} onChange={event => setRule(index, 'screenType', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs">
                  <option value="">Chọn loại màn hình</option>
                  <option value="STANDARD">STANDARD</option><option value="IMAX">IMAX</option><option value="4DX">4DX</option><option value="SCREENX">SCREENX</option>
                </select>
              )}
              {rule.scope === 'CINEMA' && <div className="rounded-lg border border-zinc-800 px-2 py-2 text-xs text-zinc-500">Áp dụng cho mọi phòng</div>}
              <select value={rule.dayType} onChange={event => setRule(index, 'dayType', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs">
                <option value="ALL_DAYS">Mọi ngày</option><option value="WEEKDAY">Ngày thường</option><option value="WEEKEND">Cuối tuần</option>
              </select>
              <input type="time" value={rule.timeBandStart} onChange={event => setRule(index, 'timeBandStart', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs" />
              <input type="time" value={rule.timeBandEnd} onChange={event => setRule(index, 'timeBandEnd', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs" />
              <input type="number" min="1" required placeholder="Giá VND" value={rule.price} onChange={event => setRule(index, 'price', event.target.value)} className="rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-2 text-xs" />
              <button type="button" disabled={form.rules.length === 1} onClick={() => setForm(current => ({ ...current, rules: current.rules.filter((_, ruleIndex) => ruleIndex !== index) }))} className="flex items-center justify-center gap-2 rounded-lg border border-red-500/20 text-xs font-bold text-red-300 disabled:opacity-30"><Trash2 className="h-4 w-4" /> Xóa</button>
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-900/40 p-5">
        <div>
          <h2 className="font-black">Xem trước phân giải hiện tại</h2>
          <p className="mt-1 text-xs text-zinc-500">Kiểm tra các chính sách đang ACTIVE cho một phòng và giờ chiếu. Bản nháp chưa lưu hoặc chưa kích hoạt không được áp dụng.</p>
        </div>
        <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
          <select value={previewInput.auditoriumId} onChange={event => setPreviewInput(current => ({ ...current, auditoriumId: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm">
            <option value="">Chọn phòng chiếu</option>
            {auditoriums.map(item => <option key={item.publicId} value={item.publicId}>{item.name}</option>)}
          </select>
          <input type="datetime-local" value={previewInput.startTime} onChange={event => setPreviewInput(current => ({ ...current, startTime: event.target.value }))} className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 py-2.5 text-sm" />
          <button type="button" disabled={previewing || !form.cinemaId || !previewInput.auditoriumId || !previewInput.startTime} onClick={runPreview} className="flex items-center justify-center gap-2 rounded-xl border border-amber-500/40 px-4 py-2.5 text-sm font-black text-amber-300 disabled:opacity-40">
            <Eye className="h-4 w-4" /> {previewing ? 'Đang phân giải…' : 'Xem trước'}
          </button>
        </div>
        {preview && (
          <div className={`rounded-xl border p-4 ${preview.complete ? 'border-emerald-500/30 bg-emerald-500/10' : 'border-red-500/30 bg-red-500/10'}`}>
            <p className={`text-sm font-black ${preview.complete ? 'text-emerald-300' : 'text-red-300'}`}>
              {preview.complete ? 'Đầy đủ' : 'Chưa đầy đủ'} · {preview.timezone}
            </p>
            <div className="mt-3 grid gap-2 md:grid-cols-2 xl:grid-cols-3">
              {preview.prices?.map(line => (
                <div key={line.seatTypeId} className="rounded-lg bg-zinc-950/70 p-3 text-xs">
                  <p className="font-bold text-zinc-200">{line.seatTypeName} ({line.seatTypeCode})</p>
                  <p className="mt-1 text-emerald-300">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: preview.currency || 'VND' }).format(line.price)}</p>
                  <p className="mt-1 truncate text-zinc-500">{line.policyName}</p>
                </div>
              ))}
            </div>
            {preview.missingSeatTypes?.length > 0 && <p className="mt-3 text-xs text-red-200">{getPricingReasonPresentation('PRICING_INCOMPLETE').label}: {preview.missingSeatTypes.map(item => item.seatTypeName).join(', ')}</p>}
            {preview.ambiguousSeatTypes?.length > 0 && <p className="mt-2 text-xs text-red-200">{getPricingReasonPresentation('PRICING_AMBIGUOUS').label}: {preview.ambiguousSeatTypes.map(item => item.seatTypeName).join(', ')}</p>}
          </div>
        )}
      </section>

      {conflicts.length > 0 && (
        <section className="rounded-2xl border border-red-500/30 bg-red-500/10 p-5">
          <h2 className="font-black text-red-300">Xung đột cùng hạng</h2>
          {conflicts.map(conflict => {
            const presentation = getConflictPresentation(conflict);
            return (
              <div key={`${conflict.firstRuleId}-${conflict.secondRuleId}`} className="mt-3 rounded-xl border border-red-500/20 bg-zinc-950/30 p-3 text-xs text-red-100">
                <p className="font-black">{presentation.title}</p>
                <p className="mt-1">{presentation.facts}</p>
                <p className="mt-2 text-red-200">{presentation.guidance}</p>
                <details className="mt-2 text-zinc-400">
                  <summary className="cursor-pointer">Chi tiết kỹ thuật</summary>
                  <p className="mt-1 font-mono">{presentation.technical.reasonCode} · {presentation.technical.ruleIds.join(' ↔ ')}</p>
                </details>
              </div>
            );
          })}
        </section>
      )}
    </form>
  );
}
