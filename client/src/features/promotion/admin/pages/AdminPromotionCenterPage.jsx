import { useCallback, useDeferredValue, useEffect, useMemo, useRef, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  BadgePercent,
  CalendarClock,
  CheckCircle2,
  Copy,
  Download,
  Eye,
  FileSpreadsheet,
  Gift,
  History,
  Loader2,
  PauseCircle,
  Plus,
  RefreshCw,
  Search,
  TicketPercent,
  Trash2,
  Upload,
  WalletCards,
  X,
} from 'lucide-react';
import SearchableSelect from '@/components/common/SearchableSelect';
import { getCustomers } from '@/features/internal-staff/admin/services/userAdminService';
import adminMovieService from '@/features/catalog/admin/services/adminMovieService';
import adminCinemaService from '@/features/facilities/admin/services/adminCinemaService';
import scoreAdminService from '@/features/score/admin/services/scoreAdminService';
import adminPromotionService from '../services/adminPromotionService';
import {
  ACTION_TYPES,
  CAMPAIGN_STATUSES,
  CREATABLE_CAMPAIGN_TYPES,
  COMPENSATION_STATUSES,
  COMPENSATION_TYPES,
  COUPON_STATUSES,
  COUPON_TYPES,
  DISTRIBUTION_TYPES,
  LEGAL_STATUSES,
  REDEMPTION_STATUSES,
  REDEMPTION_TYPES,
  RESERVATION_STATUSES,
  RULE_TYPES,
  VOUCHER_SOURCES,
  VOUCHER_STATUSES,
  VOUCHER_TYPES,
  actionFromPreset,
  badgeClass,
  conditionSummary,
  currency,
  defaultConditions,
  fieldErrors,
  formatDateTime,
  friendlyPromotionError,
  fromDateTimeLocal,
  jsonString,
  labelFor,
  normalizePage,
  safeJsonParse,
  toDateTimeLocal,
  voucherDiscountSummary,
} from '../../shared/promotionPresentation';

const tomorrowLocal = () => toDateTimeLocal(new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString());
const nextWeekLocal = () => toDateTimeLocal(new Date(Date.now() + 8 * 24 * 60 * 60 * 1000).toISOString());
const nextMonthLocal = () => toDateTimeLocal(new Date(Date.now() + 31 * 24 * 60 * 60 * 1000).toISOString());

const emptyPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, last: true };

const tabs = [
  { key: 'campaigns', label: 'Chiến dịch', icon: BadgePercent },
  { key: 'rules', label: 'Rule', icon: FileSpreadsheet },
  { key: 'coupons', label: 'Coupon', icon: TicketPercent },
  { key: 'vouchers', label: 'Voucher', icon: WalletCards },
  { key: 'compensations', label: 'Bồi thường', icon: Gift },
  { key: 'redemptions', label: 'Lịch sử dùng', icon: History },
  { key: 'reservations', label: 'Reservation', icon: CalendarClock },
];

const initialQueries = {
  campaigns: { name: '', code: '', status: '', from: '', to: '', page: 0, size: 10, sort: 'createdAt,desc' },
  rules: { campaignPublicId: '', code: '', enabled: '', page: 0, size: 10, sort: 'createdAt,desc' },
  coupons: { keyword: '', campaignPublicId: '', status: '', validAt: '', page: 0, size: 10, sort: 'createdAt,desc' },
  vouchers: { keyword: '', ownerPublicId: '', campaignPublicId: '', status: '', source: '', page: 0, size: 10, sort: 'createdAt,desc' },
  compensations: { userPublicId: '', type: '', status: '', from: '', to: '', page: 0, size: 10, sort: 'issuedAt,desc' },
  redemptions: { type: '', userPublicId: '', bookingPublicId: '', status: '', from: '', to: '', page: 0, size: 10 },
  reservations: { type: '', status: '', userPublicId: '', bookingPublicId: '', orderPublicId: '', from: '', to: '', page: 0, size: 10 },
};

const sortOptionsByTab = {
  campaigns: [
    ['createdAt,desc', 'Mới tạo gần đây'],
    ['startAt,asc', 'Bắt đầu sớm nhất'],
    ['endAt,asc', 'Kết thúc sớm nhất'],
    ['priority,asc', 'Ưu tiên cao trước'],
    ['name,asc', 'Tên A-Z'],
  ],
  rules: [
    ['createdAt,desc', 'Mới tạo gần đây'],
    ['priority,asc', 'Ưu tiên cao trước'],
    ['code,asc', 'Mã A-Z'],
    ['name,asc', 'Tên A-Z'],
  ],
  coupons: [
    ['createdAt,desc', 'Mới tạo gần đây'],
    ['validTo,asc', 'Sắp hết hạn'],
    ['priority,asc', 'Ưu tiên cao trước'],
    ['code,asc', 'Mã A-Z'],
  ],
  vouchers: [
    ['createdAt,desc', 'Mới phát hành'],
    ['validTo,asc', 'Sắp hết hạn'],
    ['code,asc', 'Mã A-Z'],
    ['status,asc', 'Theo trạng thái'],
  ],
  compensations: [
    ['issuedAt,desc', 'Mới phát hành'],
    ['expiredAt,asc', 'Sắp hết hạn'],
    ['amount,desc', 'Giá trị cao nhất'],
    ['status,asc', 'Theo trạng thái'],
  ],
};

function Badge({ status }) {
  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-black ${badgeClass(status)}`}>
      {labelFor(status)}
    </span>
  );
}

function Field({ label, required, error, hint, children }) {
  return (
    <label className="block space-y-1.5 text-xs font-bold text-zinc-400">
      <span>{label}{required && <span className="ml-1 text-brand-orange">*</span>}</span>
      {children}
      {hint && !error && <span className="block text-[10px] font-medium leading-4 text-zinc-600">{hint}</span>}
      {error && <span className="block text-[10px] font-bold leading-4 text-red-400">{error}</span>}
    </label>
  );
}

const inputClass = 'min-h-11 w-full rounded-xl border border-zinc-800 bg-zinc-950 px-3 text-sm font-medium text-zinc-100 outline-none transition-colors placeholder:text-zinc-600 focus:border-brand-orange disabled:cursor-not-allowed disabled:opacity-50';
const areaClass = 'min-h-28 w-full resize-y rounded-xl border border-zinc-800 bg-zinc-950 p-3 font-mono text-xs leading-5 text-zinc-100 outline-none transition-colors placeholder:text-zinc-600 focus:border-brand-orange';

const requiredMessage = 'Thông tin này là bắt buộc.';

const validatePeriod = (start, end, startField, endField, errors) => {
  if (!start) errors[startField] = requiredMessage;
  if (!end) errors[endField] = requiredMessage;
  if (start && end && new Date(end).getTime() <= new Date(start).getTime()) {
    errors[endField] = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.';
  }
};

const hasErrors = errors => Object.keys(errors).length > 0;

const actionTypeForVoucher = voucherType => {
  if (voucherType === 'PERCENTAGE') return 'PERCENTAGE';
  if (voucherType === 'FREE_TICKET') return 'FREE_TICKET';
  if (voucherType === 'FREE_COMBO') return 'FREE_COMBO';
  if (voucherType === 'CASHBACK') return 'CASHBACK';
  return 'FIXED_AMOUNT';
};

const listFromEnvelope = response => {
  const payload = response?.data?.data ?? response?.data ?? response;
  if (Array.isArray(payload)) return payload;
  return payload?.content || payload?.data || [];
};

const conditionValues = conditionsJson => {
  const parsed = safeJsonParse(conditionsJson, {}).value || {};
  return {
    minimumOrderAmount: parsed.minimumOrderAmount ?? parsed.minOrderAmount ?? 0,
    allowedUserIds: Array.isArray(parsed.allowedUserIds) ? parsed.allowedUserIds : [],
    movieIds: Array.isArray(parsed.movieIds) ? parsed.movieIds : [],
    cinemaIds: Array.isArray(parsed.cinemaIds) ? parsed.cinemaIds : [],
    dayOfWeek: Array.isArray(parsed.dayOfWeek) ? parsed.dayOfWeek : [],
    requiredTierCode: parsed.requiredTierCode || '',
    requiresVerification: Boolean(parsed.requiresVerification),
    allowMultipleVoucherPerOrder: Boolean(parsed.allowMultipleVoucherPerOrder),
  };
};

const conditionsFromForm = (form, currentConditions = {}) => {
  const next = { ...currentConditions };
  [
    'minimumOrderAmount',
    'minOrderAmount',
    'allowedUserIds',
    'movieIds',
    'cinemaIds',
    'dayOfWeek',
    'requiredTierCode',
    'requiresVerification',
    'allowMultipleVoucherPerOrder',
  ].forEach(key => delete next[key]);
  if (Number(form.minimumOrderAmount) > 0) next.minimumOrderAmount = Number(form.minimumOrderAmount);
  if (form.allowedUserIds?.length) next.allowedUserIds = form.allowedUserIds;
  if (form.movieIds?.length) next.movieIds = form.movieIds;
  if (form.cinemaIds?.length) next.cinemaIds = form.cinemaIds;
  if (form.dayOfWeek?.length) next.dayOfWeek = form.dayOfWeek;
  if (form.requiredTierCode) next.requiredTierCode = form.requiredTierCode;
  if (form.requiresVerification) next.requiresVerification = true;
  if (form.allowMultipleVoucherPerOrder) next.allowMultipleVoucherPerOrder = true;
  return next;
};

const actionValues = (actionsJson, fallbackType = 'PERCENTAGE', fallbackValue = 10) => {
  const parsed = safeJsonParse(actionsJson, {}).value;
  const action = Array.isArray(parsed) ? parsed[0] || {} : parsed || {};
  return {
    actionType: action.discountType || action.type || action.actionType || fallbackType,
    actionValue: action.discountValue ?? action.value ?? action.amount ?? action.percentage ?? fallbackValue,
    maxDiscountAmount: action.maxDiscountAmount ?? action.maximumDiscountAmount ?? action.maxAmount ?? 50000,
  };
};

const validateActionConfig = actions => {
  const action = Array.isArray(actions) ? actions[0] : actions;
  if (!action || typeof action !== 'object' || Array.isArray(action)) return 'Cần cấu hình đúng một giá trị giảm.';
  const type = String(action.discountType || action.type || action.actionType || '').toUpperCase();
  const value = Number(action.discountValue ?? action.value ?? action.amount ?? action.percentage);
  if (['PERCENTAGE', 'PERCENT'].includes(type) && (!Number.isFinite(value) || value <= 0 || value > 50)) {
    return 'Phần trăm giảm phải lớn hơn 0 và không vượt quá 50%.';
  }
  if (['FIXED_AMOUNT', 'AMOUNT', 'CASHBACK'].includes(type) && (!Number.isFinite(value) || value <= 0)) {
    return 'Số tiền giảm phải lớn hơn 0.';
  }
  if (!['PERCENTAGE', 'PERCENT', 'FIXED_AMOUNT', 'AMOUNT', 'CASHBACK', 'FREE', 'FREE_TICKET', 'FREE_COMBO', 'FULL_DISCOUNT'].includes(type)) {
    return 'Loại giá trị giảm chưa được backend hỗ trợ.';
  }
  return '';
};

const weekDays = [
  ['MONDAY', 'T2'],
  ['TUESDAY', 'T3'],
  ['WEDNESDAY', 'T4'],
  ['THURSDAY', 'T5'],
  ['FRIDAY', 'T6'],
  ['SATURDAY', 'T7'],
  ['SUNDAY', 'CN'],
];

const availableCampaignTransitions = campaign => {
  if (campaign.status === 'DRAFT') {
    return campaign.approvalStatus === 'APPROVED' ? ['PUBLISH', 'CANCEL'] : ['SUBMIT', 'CANCEL'];
  }
  if (campaign.status === 'SCHEDULED') return ['ACTIVATE', 'PAUSE', 'KILL_SWITCH', 'CANCEL'];
  if (campaign.status === 'ACTIVE') return ['PAUSE', 'KILL_SWITCH', 'CANCEL'];
  if (campaign.status === 'PAUSED') return ['ACTIVATE', 'KILL_SWITCH', 'CANCEL'];
  return [];
};

function SelectField({ value, onChange, options, placeholder = 'Tất cả', ...props }) {
  return (
    <select
      value={value}
      onChange={event => onChange(event.target.value)}
      className={inputClass}
      {...props}
    >
      <option value="">{placeholder}</option>
      {options.map(option => <option key={option} value={option}>{labelFor(option)}</option>)}
    </select>
  );
}

function Toggle({ checked, onChange, label, hint }) {
  return (
    <button
      type="button"
      onClick={() => onChange(!checked)}
      className={`rounded-xl border p-3 text-left transition-colors ${checked ? 'border-brand-orange/40 bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 bg-zinc-950 text-zinc-300 hover:border-zinc-700'}`}
    >
      <span className="block text-xs font-black">{label}</span>
      {hint && <span className="mt-1 block text-[10px] leading-4 text-zinc-500">{hint}</span>}
    </button>
  );
}

function Modal({ title, subtitle, children, onClose, wide = false }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="promotion-modal-title"
        className={`max-h-[92vh] w-full overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900 shadow-2xl ${wide ? 'max-w-5xl' : 'max-w-3xl'}`}
      >
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 px-5 py-4">
          <div>
            <h2 id="promotion-modal-title" className="text-lg font-black text-white">{title}</h2>
            {subtitle && <p className="mt-1 text-xs leading-5 text-zinc-500">{subtitle}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="rounded-xl p-2 text-zinc-500 transition-colors hover:bg-zinc-800 hover:text-white"
          >
            <X className="h-5 w-5" />
          </button>
        </header>
        <div className="max-h-[calc(92vh-74px)] overflow-y-auto p-5">
          {children}
        </div>
      </section>
    </div>
  );
}

function Pagination({ page, onPage }) {
  if (!page || page.totalPages <= 1) return null;
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-zinc-800 bg-zinc-950/40 px-4 py-3 text-xs text-zinc-500">
      <span className="font-bold">Trang {page.page + 1}/{page.totalPages} · {page.totalElements.toLocaleString('vi-VN')} bản ghi</span>
      <div className="flex gap-2">
        <button
          type="button"
          disabled={page.page <= 0}
          onClick={() => onPage(page.page - 1)}
          className="rounded-lg border border-zinc-700 px-3 py-2 font-bold text-zinc-300 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Trước
        </button>
        <button
          type="button"
          disabled={page.last || page.page + 1 >= page.totalPages}
          onClick={() => onPage(page.page + 1)}
          className="rounded-lg border border-zinc-700 px-3 py-2 font-bold text-zinc-300 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Tiếp
        </button>
      </div>
    </div>
  );
}

function useJsonForm(initialConditions, initialActions, initialContext) {
  const [conditionsJson, setConditionsJson] = useState(jsonString(initialConditions));
  const [actionsJson, setActionsJson] = useState(jsonString(initialActions));
  const [contextJson, setContextJson] = useState(jsonString(initialContext || {
    orderAmount: 250000,
    ticketAmount: 180000,
    comboAmount: 70000,
    channel: 'WEB',
  }));

  const parse = useCallback(() => {
    const conditions = safeJsonParse(conditionsJson);
    const actions = safeJsonParse(actionsJson);
    const context = safeJsonParse(contextJson);
    return { conditions, actions, context };
  }, [actionsJson, conditionsJson, contextJson]);

  return {
    conditionsJson,
    setConditionsJson,
    actionsJson,
    setActionsJson,
    contextJson,
    setContextJson,
    parse,
  };
}

function CampaignForm({ record, onSubmit, onCancel, saving, errors }) {
  const isEdit = Boolean(record);
  const [form, setForm] = useState({
    code: record?.code || '',
    name: record?.name || '',
    description: record?.description || '',
    campaignType: record?.campaignType || 'COUPON',
    priority: record?.priority ?? 100,
    stackable: Boolean(record?.stackable),
    exclusiveCampaign: Boolean(record?.exclusiveCampaign),
    autoActivate: record?.autoActivate ?? true,
    autoComplete: record?.autoComplete ?? true,
    autoPauseWhenBudgetExceeded: record?.autoPauseWhenBudgetExceeded ?? true,
    timezone: record?.timezone || 'Asia/Ho_Chi_Minh',
    startAt: toDateTimeLocal(record?.startAt) || tomorrowLocal(),
    endAt: toDateTimeLocal(record?.endAt) || nextWeekLocal(),
    budgetAmount: record?.budgetAmount ?? 0,
    maxRedemptions: record?.maxRedemptions ?? '',
    maxRedemptionsPerUser: record?.maxRedemptionsPerUser ?? 1,
    legalNotificationRef: record?.legalNotificationRef || '',
    remarks: record?.remarks || '',
  });

  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const submit = event => {
    event.preventDefault();
    const localErrors = {};
    if (!isEdit && !form.code.trim()) localErrors.code = requiredMessage;
    if (!form.name.trim()) localErrors.name = requiredMessage;
    if (!isEdit && !form.campaignType) localErrors.campaignType = requiredMessage;
    if (!form.timezone.trim()) localErrors.timezone = requiredMessage;
    if (Number(form.priority) < 0) localErrors.priority = 'Mức ưu tiên không được âm.';
    if (Number(form.budgetAmount) < 0) localErrors.budgetAmount = 'Ngân sách không được âm.';
    if (Number(form.maxRedemptionsPerUser) < 1) localErrors.maxRedemptionsPerUser = 'Tối thiểu 1 lượt mỗi khách.';
    if (form.maxRedemptions && Number(form.maxRedemptions) < 1) localErrors.maxRedemptions = 'Tối thiểu 1 lượt.';
    validatePeriod(form.startAt, form.endAt, 'startAt', 'endAt', localErrors);
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }

    const payload = {
      name: form.name,
      description: form.description,
      priority: Number(form.priority),
      stackable: form.stackable,
      exclusiveCampaign: form.exclusiveCampaign,
      autoActivate: form.autoActivate,
      autoComplete: form.autoComplete,
      autoPauseWhenBudgetExceeded: form.autoPauseWhenBudgetExceeded,
      timezone: form.timezone,
      startAt: fromDateTimeLocal(form.startAt),
      endAt: fromDateTimeLocal(form.endAt),
      budgetAmount: Number(form.budgetAmount || 0),
      maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser || 1),
      legalNotificationRef: form.legalNotificationRef || null,
      remarks: form.remarks,
    };
    if (!isEdit) {
      payload.code = form.code;
      payload.campaignType = form.campaignType;
    }
    onSubmit(payload);
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2">
        {!isEdit && (
          <Field label="Mã chiến dịch" required error={errors.code} hint="Chỉ chữ, số, gạch dưới hoặc gạch nối.">
            <input value={form.code} onChange={event => update('code', event.target.value.toUpperCase())} className={inputClass} placeholder="SUMMER_2026" />
          </Field>
        )}
        <Field label="Tên chiến dịch" required error={errors.name}>
          <input value={form.name} onChange={event => update('name', event.target.value)} className={inputClass} placeholder="Ưu đãi mùa hè" />
        </Field>
        {!isEdit && (
          <Field label="Loại chiến dịch" required error={errors.campaignType} hint="Checkout hiện hỗ trợ chiến dịch Coupon và Voucher.">
            <SelectField value={form.campaignType} onChange={value => update('campaignType', value)} options={CREATABLE_CAMPAIGN_TYPES} placeholder="Chọn loại" />
          </Field>
        )}
        <Field label="Mức ưu tiên" required error={errors.priority} hint="Số nhỏ hơn thường được xét trước trong vận hành.">
          <input type="number" min="0" value={form.priority} onChange={event => update('priority', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Bắt đầu" required error={errors.startAt}>
          <input type="datetime-local" value={form.startAt} onChange={event => update('startAt', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Kết thúc" required error={errors.endAt}>
          <input type="datetime-local" value={form.endAt} onChange={event => update('endAt', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Ngân sách" required error={errors.budgetAmount}>
          <input type="number" min="0" step="1000" value={form.budgetAmount} onChange={event => update('budgetAmount', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Tổng lượt dùng tối đa" error={errors.maxRedemptions} hint="Để trống nếu không giới hạn bởi field này.">
          <input type="number" min="1" value={form.maxRedemptions} onChange={event => update('maxRedemptions', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Lượt dùng mỗi khách" required error={errors.maxRedemptionsPerUser}>
          <input type="number" min="1" value={form.maxRedemptionsPerUser} onChange={event => update('maxRedemptionsPerUser', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Múi giờ" required error={errors.timezone}>
          <input value={form.timezone} onChange={event => update('timezone', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Mã thông báo pháp lý" error={errors.legalNotificationRef}>
          <input value={form.legalNotificationRef} onChange={event => update('legalNotificationRef', event.target.value)} className={inputClass} />
        </Field>
      </div>

      <Field label="Mô tả" error={errors.description}>
        <textarea value={form.description} onChange={event => update('description', event.target.value)} className={areaClass} placeholder="Nội dung dễ hiểu cho đội vận hành." />
      </Field>
      <Field label="Ghi chú nội bộ" error={errors.remarks}>
        <textarea value={form.remarks} onChange={event => update('remarks', event.target.value)} className={areaClass} />
      </Field>

      <div className="grid gap-3 md:grid-cols-3">
        <Toggle checked={form.stackable} onChange={value => update('stackable', value)} label="Cho cộng dồn" hint="Cho phép đi cùng ưu đãi khác nếu backend rule cho phép." />
        <Toggle checked={form.exclusiveCampaign} onChange={value => update('exclusiveCampaign', value)} label="Chiến dịch độc quyền" hint="Dùng khi muốn hạn chế kết hợp với ưu đãi khác." />
        <Toggle checked={form.autoPauseWhenBudgetExceeded} onChange={value => update('autoPauseWhenBudgetExceeded', value)} label="Tự dừng khi hết ngân sách" hint="Giảm rủi ro vượt ngân sách." />
        <Toggle checked={form.autoActivate} onChange={value => update('autoActivate', value)} label="Tự kích hoạt" />
        <Toggle checked={form.autoComplete} onChange={value => update('autoComplete', value)} label="Tự hoàn tất" />
      </div>

      <FormActions saving={saving} onCancel={onCancel} submitLabel={isEdit ? 'Lưu chiến dịch' : 'Tạo chiến dịch'} />
    </form>
  );
}

function RuleForm({ record, campaignOptions, customerOptions, movieOptions, cinemaOptions, tierOptions, selectedCampaignId, onSubmit, onCancel, onPreview, saving, errors }) {
  const isEdit = Boolean(record);
  const initialConditions = conditionValues(record?.conditionsJson);
  const initialActionValues = actionValues(record?.actionsJson);
  const initialAction = record?.actionsJson || actionFromPreset({ actionType: 'PERCENTAGE', actionValue: 10, maxDiscountAmount: 50000 });
  const json = useJsonForm(record?.conditionsJson || defaultConditions(0), initialAction);
  const [form, setForm] = useState({
    campaignPublicId: record?.campaignPublicId || selectedCampaignId || '',
    code: record?.code || '',
    name: record?.name || '',
    description: record?.description || '',
    ruleType: record?.ruleType || 'DISCOUNT_TICKET',
    priority: record?.priority ?? 100,
    executionOrder: record?.executionOrder ?? 1,
    stackable: Boolean(record?.stackable),
    stopFurtherRules: Boolean(record?.stopFurtherRules),
    enabled: record?.enabled ?? true,
    effectiveFrom: toDateTimeLocal(record?.effectiveFrom) || tomorrowLocal(),
    effectiveTo: toDateTimeLocal(record?.effectiveTo) || '',
    ...initialConditions,
    ...initialActionValues,
    metadataJson: jsonString(record?.metadataJson || {}),
  });
  const [preview, setPreview] = useState({ loading: false, value: null, error: '' });
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));

  const applyPreset = (changes = {}) => {
    const next = { ...form, ...changes };
    setForm(current => ({ ...current, ...changes }));
    const currentConditions = safeJsonParse(json.conditionsJson, {}).value;
    json.setConditionsJson(jsonString(conditionsFromForm(next, currentConditions)));
    json.setActionsJson(jsonString(actionFromPreset(next)));
  };

  const submit = event => {
    event.preventDefault();
    const conditions = safeJsonParse(json.conditionsJson);
    const actions = safeJsonParse(json.actionsJson);
    const metadata = safeJsonParse(form.metadataJson);
    const localErrors = {};
    if (!isEdit && !form.campaignPublicId) localErrors.campaignPublicId = requiredMessage;
    if (!isEdit && !form.code.trim()) localErrors.code = requiredMessage;
    if (!form.name.trim()) localErrors.name = requiredMessage;
    if (!isEdit && !form.ruleType) localErrors.ruleType = requiredMessage;
    if (Number(form.priority) < 0) localErrors.priority = 'Mức ưu tiên không được âm.';
    if (Number(form.executionOrder) < 1) localErrors.executionOrder = 'Thứ tự chạy phải từ 1.';
    if (!form.effectiveFrom) localErrors.effectiveFrom = requiredMessage;
    if (form.effectiveFrom && form.effectiveTo && new Date(form.effectiveTo).getTime() <= new Date(form.effectiveFrom).getTime()) {
      localErrors.effectiveTo = 'Thời điểm kết thúc phải sau thời điểm bắt đầu.';
    }
    if (conditions.error) localErrors.conditionsJson = conditions.error;
    if (actions.error) localErrors.actionsJson = actions.error;
    else if (validateActionConfig(actions.value)) localErrors.actionsJson = validateActionConfig(actions.value);
    if (metadata.error) localErrors.metadataJson = metadata.error;
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }
    const payload = {
      name: form.name,
      description: form.description,
      priority: Number(form.priority),
      executionOrder: Number(form.executionOrder),
      stackable: form.stackable,
      stopFurtherRules: form.stopFurtherRules,
      enabled: form.enabled,
      conditionsJson: JSON.stringify(conditions.value),
      actionsJson: JSON.stringify(actions.value),
      metadataJson: JSON.stringify(metadata.value),
      effectiveFrom: fromDateTimeLocal(form.effectiveFrom),
      effectiveTo: fromDateTimeLocal(form.effectiveTo),
    };
    if (!isEdit) {
      payload.campaignPublicId = form.campaignPublicId;
      payload.code = form.code;
      payload.ruleType = form.ruleType;
    }
    onSubmit(payload);
  };

  const previewRule = async () => {
    setPreview({ loading: true, value: null, error: '' });
    try {
      const value = await onPreview({
        conditionsJson: json.conditionsJson,
        actionsJson: json.actionsJson,
        contextJson: json.contextJson,
      });
      setPreview({ loading: false, value, error: '' });
    } catch (error) {
      setPreview({ loading: false, value: null, error: friendlyPromotionError(error) });
    }
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2">
        {!isEdit && (
          <Field label="Chiến dịch" required error={errors.campaignPublicId}>
            <SearchableSelect options={campaignOptions} value={form.campaignPublicId} onChange={value => update('campaignPublicId', value)} placeholder="Tìm chiến dịch" />
          </Field>
        )}
        {!isEdit && (
          <Field label="Mã rule" required error={errors.code}>
            <input value={form.code} onChange={event => update('code', event.target.value.toUpperCase())} className={inputClass} placeholder="RULE_TICKET_10" />
          </Field>
        )}
        <Field label="Tên rule" required error={errors.name}>
          <input value={form.name} onChange={event => update('name', event.target.value)} className={inputClass} />
        </Field>
        {!isEdit && (
          <Field label="Loại rule" required error={errors.ruleType}>
            <SelectField value={form.ruleType} onChange={value => update('ruleType', value)} options={RULE_TYPES} placeholder="Chọn loại" />
          </Field>
        )}
        <Field label="Ưu tiên" required error={errors.priority}>
          <input type="number" min="0" value={form.priority} onChange={event => update('priority', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Thứ tự chạy" required error={errors.executionOrder}>
          <input type="number" min="1" value={form.executionOrder} onChange={event => update('executionOrder', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Hiệu lực từ" required error={errors.effectiveFrom}>
          <input type="datetime-local" value={form.effectiveFrom} onChange={event => update('effectiveFrom', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Hiệu lực đến" error={errors.effectiveTo} hint="Để trống nếu theo campaign.">
          <input type="datetime-local" value={form.effectiveTo} onChange={event => update('effectiveTo', event.target.value)} className={inputClass} />
        </Field>
      </div>

      <Field label="Mô tả" error={errors.description}>
        <textarea value={form.description} onChange={event => update('description', event.target.value)} className={areaClass} />
      </Field>

      <BenefitJsonEditor form={form} update={update} json={json} applyPreset={applyPreset} errors={errors} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} />

      <div>
        <Field label="Context preview" hint="Dùng riêng cho API preview rule, không lưu vào rule.">
          <textarea value={json.contextJson} onChange={event => json.setContextJson(event.target.value)} className={areaClass} />
        </Field>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <Toggle checked={form.enabled} onChange={value => update('enabled', value)} label="Bật rule" />
        <Toggle checked={form.stackable} onChange={value => update('stackable', value)} label="Cho cộng dồn" />
        <Toggle checked={form.stopFurtherRules} onChange={value => update('stopFurtherRules', value)} label="Dừng rule sau" hint="Khi rule này match, engine dừng các rule tiếp theo." />
      </div>

      <div className="rounded-xl border border-zinc-800 bg-zinc-950 p-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-xs font-bold text-zinc-400">Preview giảm giá từ backend rule API</p>
          <button type="button" onClick={previewRule} disabled={preview.loading} className="inline-flex items-center gap-2 rounded-lg border border-zinc-700 px-3 py-2 text-xs font-black text-zinc-300 hover:bg-zinc-800 disabled:opacity-50">
            {preview.loading && <Loader2 className="h-3.5 w-3.5 animate-spin" />}
            Tính thử
          </button>
        </div>
        {preview.value !== null && <p className="mt-2 text-sm font-black text-emerald-300">Giảm dự kiến: {currency(preview.value)}</p>}
        {preview.error && <p className="mt-2 text-xs font-bold text-red-400">{preview.error}</p>}
      </div>

      <FormActions saving={saving} onCancel={onCancel} submitLabel={isEdit ? 'Lưu rule' : 'Tạo rule'} />
    </form>
  );
}

function CouponForm({ record, campaignOptions, customerOptions, movieOptions, cinemaOptions, tierOptions, onSubmit, onCancel, saving, errors }) {
  const isEdit = Boolean(record);
  const initialConditions = conditionValues(record?.conditionsJson);
  const initialActionValues = actionValues(record?.actionsJson);
  const json = useJsonForm(record?.conditionsJson || defaultConditions(0), record?.actionsJson || actionFromPreset({ actionType: 'PERCENTAGE', actionValue: 10, maxDiscountAmount: 50000 }));
  const [mode, setMode] = useState('single');
  const [form, setForm] = useState({
    campaignPublicId: record?.campaignPublicId || '',
    code: record?.code || '',
    prefix: 'CPN',
    quantity: 10,
    name: record?.name || '',
    description: record?.description || '',
    couponType: record?.couponType || 'PUBLIC',
    status: record?.status || 'DRAFT',
    distributionType: record?.distributionType || 'PUBLIC',
    stackable: Boolean(record?.stackable),
    transferable: Boolean(record?.transferable),
    reusable: Boolean(record?.reusable),
    autoApply: Boolean(record?.autoApply),
    priority: record?.priority ?? 100,
    maxRedemptions: record?.maxRedemptions ?? '',
    maxRedemptionsPerUser: record?.maxRedemptionsPerUser ?? 1,
    validFrom: toDateTimeLocal(record?.validFrom) || tomorrowLocal(),
    validTo: toDateTimeLocal(record?.validTo) || nextWeekLocal(),
    ...initialConditions,
    ...initialActionValues,
    metadataJson: jsonString(record?.metadataJson || {}),
  });
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const applyPreset = (changes = {}) => {
    const next = { ...form, ...changes };
    setForm(current => ({ ...current, ...changes }));
    const currentConditions = safeJsonParse(json.conditionsJson, {}).value;
    json.setConditionsJson(jsonString(conditionsFromForm(next, currentConditions)));
    json.setActionsJson(jsonString(actionFromPreset(next)));
  };
  const submit = event => {
    event.preventDefault();
    const conditions = safeJsonParse(json.conditionsJson);
    const actions = safeJsonParse(json.actionsJson);
    const metadata = safeJsonParse(form.metadataJson);
    const localErrors = {};
    if (!isEdit && !form.campaignPublicId) localErrors.campaignPublicId = requiredMessage;
    if (!isEdit && mode === 'single' && !form.code.trim()) localErrors.code = requiredMessage;
    if (!isEdit && mode === 'generate' && !form.prefix.trim()) localErrors.prefix = requiredMessage;
    if (!isEdit && mode === 'generate' && (Number(form.quantity) < 1 || Number(form.quantity) > 10000)) {
      localErrors.quantity = 'Số lượng phải từ 1 đến 10.000.';
    }
    if (!form.name.trim()) localErrors.name = requiredMessage;
    if (!form.couponType) localErrors.couponType = requiredMessage;
    if (!form.distributionType) localErrors.distributionType = requiredMessage;
    if (Number(form.priority) < 0) localErrors.priority = 'Mức ưu tiên không được âm.';
    if (Number(form.maxRedemptionsPerUser) < 1) localErrors.maxRedemptionsPerUser = 'Tối thiểu 1 lượt mỗi khách.';
    if (form.maxRedemptions && Number(form.maxRedemptions) < Number(form.maxRedemptionsPerUser)) {
      localErrors.maxRedemptionsPerUser = 'Lượt mỗi khách không thể lớn hơn tổng lượt dùng.';
    }
    if (!form.reusable && Number(form.maxRedemptionsPerUser) > 1) {
      localErrors.maxRedemptionsPerUser = 'Coupon không dùng lại chỉ được dùng 1 lần mỗi khách.';
    }
    if (
      ['PRIVATE', 'TARGETED'].includes(form.distributionType)
      || form.couponType === 'PRIVATE'
    ) {
      const allowedUsers = conditions.value?.allowedUserIds;
      if (!Array.isArray(allowedUsers) || allowedUsers.length === 0) {
        localErrors.allowedUserIds = 'Coupon riêng tư cần ít nhất một khách hàng được áp dụng.';
      }
    }
    validatePeriod(form.validFrom, form.validTo, 'validFrom', 'validTo', localErrors);
    if (conditions.error) localErrors.conditionsJson = conditions.error;
    if (actions.error) localErrors.actionsJson = actions.error;
    else if (validateActionConfig(actions.value)) localErrors.actionsJson = validateActionConfig(actions.value);
    if (metadata.error) localErrors.metadataJson = metadata.error;
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }
    const payload = {
      name: form.name,
      description: form.description,
      couponType: form.couponType,
      status: form.status,
      distributionType: form.distributionType,
      stackable: form.stackable,
      transferable: form.transferable,
      reusable: form.reusable,
      autoApply: form.autoApply,
      priority: Number(form.priority),
      maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser || 1),
      validFrom: fromDateTimeLocal(form.validFrom),
      validTo: fromDateTimeLocal(form.validTo),
      conditionsJson: conditions.value,
      actionsJson: actions.value,
      metadataJson: metadata.value,
    };
    if (!isEdit) {
      payload.campaignPublicId = form.campaignPublicId;
      if (mode === 'generate') {
        payload.prefix = form.prefix;
        payload.quantity = Number(form.quantity || 1);
      } else {
        payload.code = form.code;
      }
    }
    onSubmit(payload, null, mode);
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      {!isEdit && (
        <div className="inline-flex rounded-xl border border-zinc-800 bg-zinc-950 p-1">
          <button type="button" onClick={() => setMode('single')} className={`rounded-lg px-3 py-2 text-xs font-black ${mode === 'single' ? 'bg-brand-orange text-zinc-950' : 'text-zinc-400'}`}>Một mã</button>
          <button type="button" onClick={() => setMode('generate')} className={`rounded-lg px-3 py-2 text-xs font-black ${mode === 'generate' ? 'bg-brand-orange text-zinc-950' : 'text-zinc-400'}`}>Sinh hàng loạt</button>
        </div>
      )}
      <div className="grid gap-4 md:grid-cols-2">
        {!isEdit && (
          <Field label="Chiến dịch" required error={errors.campaignPublicId}>
            <SearchableSelect options={campaignOptions} value={form.campaignPublicId} onChange={value => update('campaignPublicId', value)} placeholder="Chọn campaign coupon" />
          </Field>
        )}
        {!isEdit && mode === 'single' && (
          <Field label="Mã coupon" required error={errors.code}>
            <input value={form.code} onChange={event => update('code', event.target.value.toUpperCase())} className={inputClass} />
          </Field>
        )}
        {!isEdit && mode === 'generate' && (
          <>
            <Field label="Tiền tố mã" error={errors.prefix}>
              <input value={form.prefix} onChange={event => update('prefix', event.target.value.toUpperCase())} className={inputClass} />
            </Field>
            <Field label="Số lượng" required error={errors.quantity}>
              <input type="number" min="1" max="10000" value={form.quantity} onChange={event => update('quantity', event.target.value)} className={inputClass} />
            </Field>
          </>
        )}
        <Field label="Tên coupon" required error={errors.name}>
          <input value={form.name} onChange={event => update('name', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Loại coupon" required error={errors.couponType}>
          <SelectField
            value={form.couponType}
            onChange={value => setForm(current => value === 'SINGLE_USE'
              ? { ...current, couponType: value, maxRedemptions: 1, maxRedemptionsPerUser: 1, reusable: false }
              : { ...current, couponType: value })}
            options={COUPON_TYPES}
            placeholder="Chọn loại"
          />
        </Field>
        <Field label="Trạng thái" error={errors.status}>
          <SelectField value={form.status} onChange={value => update('status', value)} options={COUPON_STATUSES} placeholder="Chọn trạng thái" />
        </Field>
        <Field label="Phân phối" required error={errors.distributionType}>
          <SelectField value={form.distributionType} onChange={value => update('distributionType', value)} options={DISTRIBUTION_TYPES} placeholder="Chọn kiểu" />
        </Field>
        <Field label="Hiệu lực từ" required error={errors.validFrom}>
          <input type="datetime-local" value={form.validFrom} onChange={event => update('validFrom', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Hiệu lực đến" required error={errors.validTo}>
          <input type="datetime-local" value={form.validTo} onChange={event => update('validTo', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Tổng lượt dùng" error={errors.maxRedemptions}>
          <input type="number" min="1" value={form.maxRedemptions} onChange={event => update('maxRedemptions', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Lượt dùng mỗi khách" required error={errors.maxRedemptionsPerUser}>
          <input
            type="number"
            min="1"
            value={form.maxRedemptionsPerUser}
            onChange={event => setForm(current => ({
              ...current,
              maxRedemptionsPerUser: event.target.value,
              reusable: Number(event.target.value) > 1 ? true : current.reusable,
            }))}
            className={inputClass}
          />
        </Field>
      </div>
      <BenefitJsonEditor form={form} update={update} json={json} applyPreset={applyPreset} errors={errors} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} />
      <div className="grid gap-3 md:grid-cols-4">
        <Toggle checked={form.stackable} onChange={value => update('stackable', value)} label="Cộng dồn" />
        <Toggle checked={form.transferable} onChange={value => update('transferable', value)} label="Có thể chuyển" />
        <Toggle checked={form.reusable} onChange={value => update('reusable', value)} label="Dùng lại" />
        <Toggle checked={form.autoApply} onChange={value => update('autoApply', value)} label="Tự áp dụng" />
      </div>
      <Field label="Mô tả" error={errors.description}>
        <textarea value={form.description} onChange={event => update('description', event.target.value)} className={areaClass} />
      </Field>
      <FormActions saving={saving} onCancel={onCancel} submitLabel={isEdit ? 'Lưu coupon' : mode === 'generate' ? 'Sinh coupon' : 'Tạo coupon'} />
    </form>
  );
}

function MultiReferencePicker({ options, values = [], onChange, placeholder }) {
  const availableOptions = options.filter(option => !values.includes(option.value));
  return (
    <div className="space-y-2">
      <SearchableSelect
        options={availableOptions}
        value=""
        onChange={value => value && onChange([...values, value])}
        placeholder={placeholder}
      />
      {values.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {values.map(value => {
            const option = options.find(item => item.value === value);
            return (
              <span key={value} className="inline-flex max-w-full items-center gap-1 rounded-lg border border-zinc-700 bg-zinc-900 px-2 py-1 text-[10px] font-bold text-zinc-300">
                <span className="truncate">{option?.label || value}</span>
                <button type="button" aria-label={`Bỏ ${option?.label || value}`} onClick={() => onChange(values.filter(item => item !== value))} className="shrink-0 text-zinc-500 hover:text-white">
                  <X className="h-3 w-3" />
                </button>
              </span>
            );
          })}
        </div>
      )}
    </div>
  );
}

function BenefitJsonEditor({
  form,
  update,
  json,
  applyPreset,
  errors,
  customerOptions = [],
  movieOptions = [],
  cinemaOptions = [],
  tierOptions = [],
  lockActionType = false,
}) {
  const changePreset = (key, value) => applyPreset({ [key]: value });
  const hasNumericValue = ['PERCENTAGE', 'FIXED_AMOUNT', 'CASHBACK'].includes(form.actionType);
  return (
    <section className="space-y-4 rounded-2xl border border-zinc-800 bg-zinc-950/50 p-4">
      <div>
        <h3 className="text-sm font-black text-white">Điều kiện và giá trị giảm</h3>
        <p className="mt-1 text-xs text-zinc-500">Các lựa chọn bên dưới được đồng bộ ngay vào cấu hình Promotion.</p>
      </div>
      <div className="grid gap-3 md:grid-cols-4">
        <Field label="Đơn tối thiểu">
          <input type="number" min="0" step="1000" value={form.minimumOrderAmount} onChange={event => changePreset('minimumOrderAmount', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Loại ưu đãi">
          <SelectField value={form.actionType} onChange={value => changePreset('actionType', value)} options={ACTION_TYPES} placeholder="Chọn" disabled={lockActionType} />
        </Field>
        <Field label="Giá trị">
          <input type="number" min="0" max={form.actionType === 'PERCENTAGE' ? 50 : undefined} step={form.actionType === 'PERCENTAGE' ? 1 : 1000} value={form.actionValue} onChange={event => changePreset('actionValue', event.target.value)} disabled={!hasNumericValue} className={inputClass} />
        </Field>
        <Field label="Trần giảm">
          <input type="number" min="0" step="1000" value={form.maxDiscountAmount} onChange={event => changePreset('maxDiscountAmount', event.target.value)} disabled={form.actionType !== 'PERCENTAGE'} className={inputClass} />
        </Field>
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Field label="Khách hàng được áp dụng" hint="Để trống nếu áp dụng cho mọi khách hàng." error={errors.allowedUserIds}>
          <MultiReferencePicker options={customerOptions} values={form.allowedUserIds} onChange={value => changePreset('allowedUserIds', value)} placeholder="Thêm khách hàng" />
        </Field>
        <Field label="Phim được áp dụng" hint="Để trống nếu áp dụng cho mọi phim.">
          <MultiReferencePicker options={movieOptions} values={form.movieIds} onChange={value => changePreset('movieIds', value)} placeholder="Thêm phim" />
        </Field>
        <Field label="Rạp được áp dụng" hint="Để trống nếu áp dụng tại mọi rạp.">
          <MultiReferencePicker options={cinemaOptions} values={form.cinemaIds} onChange={value => changePreset('cinemaIds', value)} placeholder="Thêm rạp" />
        </Field>
        <Field label="Hạng thành viên">
          <SearchableSelect options={tierOptions} value={form.requiredTierCode} onChange={value => changePreset('requiredTierCode', value)} placeholder="Mọi hạng thành viên" />
        </Field>
      </div>

      <Field label="Ngày áp dụng">
        <div className="grid grid-cols-4 gap-2 sm:grid-cols-7">
          {weekDays.map(([value, label]) => {
            const checked = form.dayOfWeek?.includes(value);
            return (
              <button
                key={value}
                type="button"
                aria-pressed={checked}
                onClick={() => changePreset('dayOfWeek', checked ? form.dayOfWeek.filter(day => day !== value) : [...(form.dayOfWeek || []), value])}
                className={`h-9 rounded-lg border text-xs font-black transition-colors ${checked ? 'border-brand-orange/50 bg-brand-orange/10 text-brand-orange' : 'border-zinc-800 text-zinc-500 hover:border-zinc-700 hover:text-zinc-300'}`}
              >
                {label}
              </button>
            );
          })}
        </div>
      </Field>

      <div className="grid gap-3 sm:grid-cols-2">
        <Toggle checked={form.requiresVerification} onChange={value => changePreset('requiresVerification', value)} label="Yêu cầu xác thực khách hàng" />
        <Toggle checked={form.allowMultipleVoucherPerOrder} onChange={value => changePreset('allowMultipleVoucherPerOrder', value)} label="Cho nhiều voucher trong một đơn" />
      </div>

      <details className="border-t border-zinc-800 pt-4">
        <summary className="cursor-pointer text-xs font-black text-zinc-400 hover:text-white">Cấu hình nâng cao</summary>
        <div className="mt-4 grid gap-4 lg:grid-cols-3">
          <Field label="conditionsJson" required error={errors.conditionsJson}>
            <textarea value={json.conditionsJson} onChange={event => json.setConditionsJson(event.target.value)} className={areaClass} />
          </Field>
          <Field label="actionsJson" required error={errors.actionsJson}>
            <textarea value={json.actionsJson} onChange={event => json.setActionsJson(event.target.value)} className={areaClass} />
          </Field>
          <Field label="metadataJson" error={errors.metadataJson}>
            <textarea value={form.metadataJson} onChange={event => update('metadataJson', event.target.value)} className={areaClass} />
          </Field>
        </div>
      </details>
    </section>
  );
}

function VoucherForm({ record, campaignOptions, customerOptions, movieOptions, cinemaOptions, tierOptions, onSubmit, onCancel, saving, errors }) {
  const isEdit = Boolean(record);
  const initialConditions = conditionValues(record?.conditionsJson);
  const initialActionValues = actionValues(
    record?.actionsJson,
    actionTypeForVoucher(record?.voucherType),
    record?.voucherType === 'PERCENTAGE' ? 10 : record?.faceValue || 50000
  );
  const json = useJsonForm(
    record?.conditionsJson || defaultConditions(record?.minimumOrderAmount || 0),
    record?.actionsJson || actionFromPreset({
      actionType: actionTypeForVoucher(record?.voucherType),
      actionValue: record?.voucherType === 'PERCENTAGE' ? 10 : record?.faceValue || 50000,
    })
  );
  const [form, setForm] = useState({
    campaignPublicId: record?.campaignPublicId || '',
    ownerPublicId: record?.ownerPublicId || '',
    batchOwnerPublicIds: '',
    code: record?.code || '',
    name: record?.name || '',
    description: record?.description || '',
    voucherType: record?.voucherType || 'FIXED_AMOUNT',
    source: record?.source || 'MANUAL',
    status: record?.status || 'ISSUED',
    issueReason: record?.issueReason || '',
    validFrom: toDateTimeLocal(record?.validFrom) || tomorrowLocal(),
    validTo: toDateTimeLocal(record?.validTo) || nextMonthLocal(),
    transferable: Boolean(record?.transferable),
    stackable: Boolean(record?.stackable),
    reusable: Boolean(record?.reusable),
    maxUsage: record?.maxUsage ?? 1,
    faceValue: record?.faceValue ?? 50000,
    ...initialConditions,
    minimumOrderAmount: record?.minimumOrderAmount ?? initialConditions.minimumOrderAmount,
    ...initialActionValues,
    metadataJson: jsonString(record?.metadataJson || {}),
  });
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const updateVoucherType = value => {
    const actionType = actionTypeForVoucher(value);
    const actionValue = actionType === 'PERCENTAGE' ? 10 : Number(form.faceValue || 50000);
    setForm(current => ({ ...current, voucherType: value, actionType, actionValue }));
    json.setActionsJson(jsonString(actionFromPreset({
      actionType,
      actionValue,
      maxDiscountAmount: form.maxDiscountAmount,
    })));
  };
  const applyPreset = (changes = {}) => {
    const next = { ...form, ...changes };
    setForm(current => ({ ...current, ...changes }));
    const currentConditions = safeJsonParse(json.conditionsJson, {}).value;
    json.setConditionsJson(jsonString(conditionsFromForm(next, currentConditions)));
    json.setActionsJson(jsonString(actionFromPreset(next)));
  };
  const submit = event => {
    event.preventDefault();
    const conditions = safeJsonParse(json.conditionsJson);
    const actions = safeJsonParse(json.actionsJson);
    const metadata = safeJsonParse(form.metadataJson);
    const batchOwners = form.batchOwnerPublicIds
      .split(/\r?\n|,/)
      .map(value => value.trim())
      .filter(Boolean);
    const localErrors = {};
    if (!isEdit && !form.ownerPublicId && batchOwners.length === 0) localErrors.ownerPublicId = requiredMessage;
    if (!form.name.trim()) localErrors.name = requiredMessage;
    if (!form.voucherType) localErrors.voucherType = requiredMessage;
    if (!isEdit && !form.source) localErrors.source = requiredMessage;
    if (!isEdit) validatePeriod(form.validFrom, form.validTo, 'validFrom', 'validTo', localErrors);
    if (Number(form.maxUsage) < 1) localErrors.maxUsage = 'Số lần dùng phải từ 1.';
    if (!form.reusable && Number(form.maxUsage) > 1) localErrors.maxUsage = 'Voucher không dùng lại chỉ được dùng 1 lần.';
    if (form.voucherType === 'FIXED_AMOUNT' && Number(form.faceValue) <= 0) {
      localErrors.faceValue = 'Voucher giảm tiền cần mệnh giá lớn hơn 0.';
    }
    if (Number(form.minimumOrderAmount) < 0) localErrors.minimumOrderAmount = 'Giá trị đơn tối thiểu không được âm.';
    if (conditions.error) localErrors.conditionsJson = conditions.error;
    if (actions.error) localErrors.actionsJson = actions.error;
    else if (validateActionConfig(actions.value)) localErrors.actionsJson = validateActionConfig(actions.value);
    if (metadata.error) localErrors.metadataJson = metadata.error;
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }
    const payload = {
      name: form.name,
      description: form.description,
      voucherType: form.voucherType,
      issueReason: form.issueReason,
      transferable: form.transferable,
      stackable: form.stackable,
      reusable: form.reusable,
      maxUsage: Number(form.maxUsage || 1),
      faceValue: Number(form.faceValue || 0),
      minimumOrderAmount: Number(form.minimumOrderAmount || 0),
      conditionsJson: conditions.value,
      actionsJson: actions.value,
      metadataJson: metadata.value,
    };
    if (isEdit) {
      payload.status = form.status;
    } else {
      payload.campaignPublicId = form.campaignPublicId || null;
      payload.ownerPublicId = form.ownerPublicId;
      payload.code = form.code || null;
      payload.source = form.source;
      payload.validFrom = fromDateTimeLocal(form.validFrom);
      payload.validTo = fromDateTimeLocal(form.validTo);
    }
    onSubmit(payload, null, isEdit ? 'edit' : batchOwners.length > 0 ? 'batch' : 'single', batchOwners);
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2">
        {!isEdit && (
          <>
            <Field label="Khách hàng nhận voucher" required error={errors.ownerPublicId} hint="Chọn từ danh sách hoặc nhập account ID/UUID nếu chưa tải được khách hàng.">
              <SearchableSelect options={customerOptions} value={form.ownerPublicId} onChange={value => update('ownerPublicId', value)} placeholder="Tìm khách hàng" />
              <input value={form.ownerPublicId} onChange={event => update('ownerPublicId', event.target.value)} className={`${inputClass} mt-2`} placeholder="Account ID hoặc UUID" />
            </Field>
            <Field label="Phát hành hàng loạt" hint="Mỗi dòng một account ID/UUID. Khi có dữ liệu, hệ thống dùng API batch.">
              <textarea value={form.batchOwnerPublicIds} onChange={event => update('batchOwnerPublicIds', event.target.value)} className={areaClass} placeholder="1001&#10;1002" />
            </Field>
            <Field label="Chiến dịch liên kết">
              <SearchableSelect options={campaignOptions} value={form.campaignPublicId} onChange={value => update('campaignPublicId', value)} placeholder="Không bắt buộc" />
            </Field>
            <Field label="Mã voucher" hint="Để trống để backend tự sinh nếu service hỗ trợ.">
              <input value={form.code} onChange={event => update('code', event.target.value.toUpperCase())} className={inputClass} />
            </Field>
          </>
        )}
        <Field label="Tên voucher" required error={errors.name}>
          <input value={form.name} onChange={event => update('name', event.target.value)} className={inputClass} />
        </Field>
        <Field label="Loại voucher" required error={errors.voucherType}>
          <SelectField value={form.voucherType} onChange={updateVoucherType} options={VOUCHER_TYPES} placeholder="Chọn loại" />
        </Field>
        {!isEdit && (
          <Field label="Nguồn phát hành" required error={errors.source}>
            <SelectField value={form.source} onChange={value => update('source', value)} options={VOUCHER_SOURCES} placeholder="Chọn nguồn" />
          </Field>
        )}
        {isEdit && (
          <Field label="Trạng thái" error={errors.status}>
            <SelectField value={form.status} onChange={value => update('status', value)} options={VOUCHER_STATUSES} placeholder="Chọn trạng thái" />
          </Field>
        )}
        {!isEdit && (
          <>
            <Field label="Hiệu lực từ" required error={errors.validFrom}>
              <input type="datetime-local" value={form.validFrom} onChange={event => update('validFrom', event.target.value)} className={inputClass} />
            </Field>
            <Field label="Hiệu lực đến" required error={errors.validTo}>
              <input type="datetime-local" value={form.validTo} onChange={event => update('validTo', event.target.value)} className={inputClass} />
            </Field>
          </>
        )}
        <Field label="Mệnh giá" error={errors.faceValue}>
          <input
            type="number"
            min="0"
            step="1000"
            value={form.faceValue}
            onChange={event => {
              const value = event.target.value;
              if (['FIXED_AMOUNT', 'CASHBACK'].includes(form.actionType)) applyPreset({ faceValue: value, actionValue: value });
              else update('faceValue', value);
            }}
            className={inputClass}
          />
        </Field>
        <Field label="Đơn tối thiểu" error={errors.minimumOrderAmount}>
          <input type="number" min="0" step="1000" value={form.minimumOrderAmount} onChange={event => applyPreset({ minimumOrderAmount: event.target.value })} className={inputClass} />
        </Field>
        <Field label="Số lần dùng" required error={errors.maxUsage}>
          <input
            type="number"
            min="1"
            value={form.maxUsage}
            onChange={event => setForm(current => ({
              ...current,
              maxUsage: event.target.value,
              reusable: Number(event.target.value) > 1 ? true : current.reusable,
            }))}
            className={inputClass}
          />
        </Field>
      </div>
      <BenefitJsonEditor form={form} update={update} json={json} applyPreset={applyPreset} errors={errors} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} lockActionType={['PERCENTAGE', 'FIXED_AMOUNT', 'FREE_TICKET', 'FREE_COMBO', 'CASHBACK'].includes(form.voucherType)} />
      <div className="grid gap-3 md:grid-cols-3">
        <Toggle checked={form.transferable} onChange={value => update('transferable', value)} label="Có thể chuyển" />
        <Toggle checked={form.stackable} onChange={value => update('stackable', value)} label="Cộng dồn" />
        <Toggle checked={form.reusable} onChange={value => update('reusable', value)} label="Dùng lại" />
      </div>
      <Field label="Lý do phát hành" error={errors.issueReason}>
        <textarea value={form.issueReason} onChange={event => update('issueReason', event.target.value)} className={areaClass} />
      </Field>
      <Field label="Mô tả" error={errors.description}>
        <textarea value={form.description} onChange={event => update('description', event.target.value)} className={areaClass} />
      </Field>
      <FormActions saving={saving} onCancel={onCancel} submitLabel={isEdit ? 'Lưu voucher' : 'Phát hành voucher'} />
    </form>
  );
}

function CompensationForm({ record, customerOptions, onSubmit, onCancel, saving, errors }) {
  const isEdit = Boolean(record);
  const [form, setForm] = useState({
    reservationPublicId: record?.reservationPublicId || '',
    bookingPublicId: record?.bookingPublicId || '',
    orderPublicId: record?.orderPublicId || '',
    userPublicId: record?.userPublicId || '',
    compensationType: record?.compensationType || 'CUSTOMER_SERVICE',
    reason: record?.reason || '',
    amount: record?.amount ?? 50000,
    status: record?.status || 'ISSUED',
    expiredAt: toDateTimeLocal(record?.expiredAt) || nextMonthLocal(),
    voucherCode: '',
    voucherName: record?.voucher?.name || '',
    metadataJson: jsonString(record?.metadataJson || {}),
  });
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const submit = event => {
    event.preventDefault();
    const metadata = safeJsonParse(form.metadataJson);
    const localErrors = {};
    if (!form.reason.trim()) localErrors.reason = requiredMessage;
    if (!form.expiredAt) {
      localErrors.expiredAt = requiredMessage;
    } else if (new Date(form.expiredAt).getTime() <= Date.now()) {
      localErrors.expiredAt = 'Hạn voucher phải ở tương lai.';
    }
    if (!isEdit && !form.userPublicId) localErrors.userPublicId = requiredMessage;
    if (!isEdit && !form.compensationType) localErrors.compensationType = requiredMessage;
    if (!isEdit && Number(form.amount) <= 0) localErrors.amount = 'Số tiền phải lớn hơn 0.';
    if (metadata.error) localErrors.metadataJson = metadata.error;
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }
    const payload = {
      reason: form.reason,
      expiredAt: fromDateTimeLocal(form.expiredAt),
      metadataJson: metadata.value,
    };
    if (isEdit) {
      payload.status = form.status;
    } else {
      payload.reservationPublicId = form.reservationPublicId || null;
      payload.bookingPublicId = form.bookingPublicId || null;
      payload.orderPublicId = form.orderPublicId || null;
      payload.userPublicId = form.userPublicId;
      payload.compensationType = form.compensationType;
      payload.amount = Number(form.amount || 0);
      payload.voucherCode = form.voucherCode || null;
      payload.voucherName = form.voucherName || null;
    }
    onSubmit(payload);
  };

  return (
    <form onSubmit={submit} className="space-y-5">
      <div className="grid gap-4 md:grid-cols-2">
        {!isEdit && (
          <>
            <Field label="Khách hàng" required error={errors.userPublicId}>
              <SearchableSelect options={customerOptions} value={form.userPublicId} onChange={value => update('userPublicId', value)} placeholder="Tìm khách hàng" />
              <input value={form.userPublicId} onChange={event => update('userPublicId', event.target.value)} className={`${inputClass} mt-2`} placeholder="Account ID hoặc UUID" />
            </Field>
            <Field label="Loại bồi thường" required error={errors.compensationType}>
              <SelectField value={form.compensationType} onChange={value => update('compensationType', value)} options={COMPENSATION_TYPES} placeholder="Chọn loại" />
            </Field>
            <Field label="Số tiền" required error={errors.amount}>
              <input type="number" min="1" step="1000" value={form.amount} onChange={event => update('amount', event.target.value)} className={inputClass} />
            </Field>
            <Field label="Mã booking">
              <input value={form.bookingPublicId} onChange={event => update('bookingPublicId', event.target.value)} className={inputClass} />
            </Field>
            <Field label="Mã reservation">
              <input value={form.reservationPublicId} onChange={event => update('reservationPublicId', event.target.value)} className={inputClass} />
            </Field>
            <Field label="Mã order">
              <input value={form.orderPublicId} onChange={event => update('orderPublicId', event.target.value)} className={inputClass} />
            </Field>
            <Field label="Mã voucher tùy chọn">
              <input value={form.voucherCode} onChange={event => update('voucherCode', event.target.value.toUpperCase())} className={inputClass} />
            </Field>
            <Field label="Tên voucher">
              <input value={form.voucherName} onChange={event => update('voucherName', event.target.value)} className={inputClass} />
            </Field>
          </>
        )}
        {isEdit && (
          <Field label="Trạng thái" error={errors.status}>
            <SelectField value={form.status} onChange={value => update('status', value)} options={COMPENSATION_STATUSES} placeholder="Chọn trạng thái" />
          </Field>
        )}
        <Field label="Hết hạn" required error={errors.expiredAt}>
          <input type="datetime-local" value={form.expiredAt} onChange={event => update('expiredAt', event.target.value)} className={inputClass} />
        </Field>
      </div>
      <Field label="Lý do" required error={errors.reason}>
        <textarea value={form.reason} onChange={event => update('reason', event.target.value)} className={areaClass} />
      </Field>
      <Field label="metadataJson" error={errors.metadataJson}>
        <textarea value={form.metadataJson} onChange={event => update('metadataJson', event.target.value)} className={areaClass} />
      </Field>
      <FormActions saving={saving} onCancel={onCancel} submitLabel={isEdit ? 'Lưu bồi thường' : 'Phát hành bồi thường'} />
    </form>
  );
}

function FormActions({ saving, onCancel, submitLabel }) {
  return (
    <div className="flex flex-wrap justify-end gap-3 border-t border-zinc-800 pt-5">
      <button type="button" onClick={onCancel} className="rounded-xl border border-zinc-700 px-4 py-2.5 text-sm font-bold text-zinc-300 hover:bg-zinc-800">
        Quay lại
      </button>
      <button type="submit" disabled={saving} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-zinc-950 hover:bg-amber-400 disabled:cursor-not-allowed disabled:opacity-50">
        {saving && <Loader2 className="h-4 w-4 animate-spin" />}
        {submitLabel}
      </button>
    </div>
  );
}

export default function AdminPromotionCenterPage() {
  const { triggerToast, triggerConfirm, triggerPrompt } = useOutletContext() || {};
  const [activeTab, setActiveTab] = useState('campaigns');
  const [queries, setQueries] = useState(initialQueries);
  const [pages, setPages] = useState({
    campaigns: emptyPage,
    rules: emptyPage,
    coupons: emptyPage,
    vouchers: emptyPage,
    compensations: emptyPage,
    redemptions: emptyPage,
    reservations: emptyPage,
  });
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState(null);
  const [detail, setDetail] = useState(null);
  const [saving, setSaving] = useState(false);
  const [formErrors, setFormErrors] = useState({});
  const [references, setReferences] = useState({ campaigns: [], customers: [], movies: [], cinemas: [], tiers: [] });
  const deferredQueries = useDeferredValue(queries);
  const loadSequenceRef = useRef(0);

  const campaignOptions = useMemo(() => references.campaigns.map(campaign => ({
    value: campaign.publicId,
    label: `${campaign.name} (${campaign.code})`,
    subtitle: `${labelFor(campaign.campaignType)} · ${labelFor(campaign.status)}`,
    badge: labelFor(campaign.status),
  })), [references.campaigns]);

  const customerOptions = useMemo(() => references.customers.map(customer => ({
    value: String(customer.accountId ?? customer.id ?? customer.publicId),
    label: customer.fullName || customer.email || customer.customerCode || String(customer.accountId ?? customer.id),
    subtitle: [customer.customerCode, customer.email, customer.phoneNumber].filter(Boolean).join(' · '),
    badge: customer.status,
  })), [references.customers]);

  const movieOptions = useMemo(() => references.movies.map(movie => ({
    value: String(movie.publicId || movie.id),
    label: movie.title || movie.name,
    subtitle: [movie.releaseDate, movie.status].filter(Boolean).join(' · '),
  })), [references.movies]);

  const cinemaOptions = useMemo(() => references.cinemas.map(cinema => ({
    value: String(cinema.publicId || cinema.id),
    label: cinema.name,
    subtitle: cinema.address || cinema.city,
  })), [references.cinemas]);

  const tierOptions = useMemo(() => references.tiers.map(tier => ({
    value: String(tier.code || tier.tierCode),
    label: tier.name || tier.tierName || tier.code,
    subtitle: tier.description,
  })), [references.tiers]);

  const currentQuery = queries[activeTab];
  const currentPage = pages[activeTab] || emptyPage;

  const updateQuery = (key, value) => {
    setQueries(current => ({
      ...current,
      [activeTab]: {
        ...current[activeTab],
        [key]: value,
        page: key === 'page' ? value : 0,
      },
    }));
  };

  const cleanDateParams = query => ({
    ...query,
    from: fromDateTimeLocal(query.from) || undefined,
    to: fromDateTimeLocal(query.to) || undefined,
    validAt: fromDateTimeLocal(query.validAt) || undefined,
  });

  const loadReferences = useCallback(async () => {
    const [campaigns, customers, movies, cinemas, tiers] = await Promise.all([
      adminPromotionService.searchCampaigns({ page: 0, size: 100, sort: 'createdAt,desc' }).catch(() => emptyPage),
      getCustomers({ page: 0, size: 100 }).catch(() => ({ content: [] })),
      adminMovieService.getMovies({ page: 0, size: 100 }).catch(() => []),
      adminCinemaService.getCinemas({ page: 0, size: 100 }).catch(() => []),
      scoreAdminService.getAllTiers().catch(() => []),
    ]);
    setReferences({
      campaigns: normalizePage(campaigns, 100).content,
      customers: listFromEnvelope(customers),
      movies: listFromEnvelope(movies),
      cinemas: listFromEnvelope(cinemas),
      tiers: listFromEnvelope(tiers),
    });
  }, []);

  const loadTab = useCallback(async () => {
    const requestSequence = loadSequenceRef.current + 1;
    loadSequenceRef.current = requestSequence;
    const tab = activeTab;
    const query = deferredQueries[tab];
    setLoading(true);
    try {
      let page;
      if (tab === 'campaigns') page = await adminPromotionService.searchCampaigns(cleanDateParams(query));
      if (tab === 'rules') page = await adminPromotionService.searchRules(query);
      if (tab === 'coupons') page = await adminPromotionService.searchCoupons(cleanDateParams(query));
      if (tab === 'vouchers') page = await adminPromotionService.searchVouchers(query);
      if (tab === 'compensations') page = await adminPromotionService.searchCompensations(cleanDateParams(query));
      if (tab === 'redemptions') page = await adminPromotionService.searchRedemptions(cleanDateParams(query));
      if (tab === 'reservations') page = await adminPromotionService.searchReservations(cleanDateParams(query));
      if (requestSequence !== loadSequenceRef.current) return;
      setPages(current => ({ ...current, [tab]: normalizePage(page, query.size) }));
    } catch (error) {
      if (requestSequence !== loadSequenceRef.current) return;
      triggerToast?.(friendlyPromotionError(error), 'error');
      setPages(current => ({ ...current, [tab]: emptyPage }));
    } finally {
      if (requestSequence === loadSequenceRef.current) setLoading(false);
    }
  }, [activeTab, deferredQueries, triggerToast]);

  useEffect(() => {
    // Initial reference data is loaded from Promotion and User services.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadReferences();
  }, [loadReferences]);

  useEffect(() => {
    // The active tab owns its server-side filters and page state.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTab();
  }, [loadTab]);

  const refreshAll = async () => {
    await Promise.all([loadReferences(), loadTab()]);
  };

  const closeModal = () => {
    setModal(null);
    setFormErrors({});
  };

  const submitWithFeedback = async (action, successMessage) => {
    setSaving(true);
    setFormErrors({});
    try {
      await action();
      triggerToast?.(successMessage);
      closeModal();
      await refreshAll();
    } catch (error) {
      setFormErrors(fieldErrors(error));
      triggerToast?.(friendlyPromotionError(error), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleCampaignSubmit = (payload, localErrors) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    const record = modal?.record;
    submitWithFeedback(
      () => record
        ? adminPromotionService.updateCampaign(record.publicId, payload)
        : adminPromotionService.createCampaign(payload),
      record ? 'Đã cập nhật chiến dịch.' : 'Đã tạo chiến dịch.'
    );
  };

  const handleRuleSubmit = (payload, localErrors) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    const record = modal?.record;
    submitWithFeedback(
      () => record
        ? adminPromotionService.updateRule(record.publicId, payload)
        : adminPromotionService.createRule(payload),
      record ? 'Đã cập nhật rule.' : 'Đã tạo rule.'
    );
  };

  const handleCouponSubmit = (payload, localErrors, mode) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    const record = modal?.record;
    submitWithFeedback(
      () => record
        ? adminPromotionService.updateCoupon(record.publicId, payload)
        : mode === 'generate'
          ? adminPromotionService.generateCoupons(payload)
          : adminPromotionService.createCoupon(payload),
      record ? 'Đã cập nhật coupon.' : mode === 'generate' ? 'Đã sinh coupon.' : 'Đã tạo coupon.'
    );
  };

  const handleVoucherSubmit = (payload, localErrors, mode, batchOwners = []) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    const record = modal?.record;
    submitWithFeedback(
      () => record
        ? adminPromotionService.updateVoucher(record.publicId, payload)
        : mode === 'batch'
          ? adminPromotionService.batchIssueVouchers(batchOwners.map(ownerPublicId => ({ ...payload, ownerPublicId, code: null })))
          : adminPromotionService.issueVoucher(payload),
      record ? 'Đã cập nhật voucher.' : 'Đã phát hành voucher.'
    );
  };

  const handleCompensationSubmit = (payload, localErrors) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    const record = modal?.record;
    submitWithFeedback(
      () => record
        ? adminPromotionService.updateCompensation(record.publicId, payload)
        : adminPromotionService.issueCompensation(payload),
      record ? 'Đã cập nhật bồi thường.' : 'Đã phát hành voucher bồi thường.'
    );
  };

  const openCampaignDetail = async campaign => {
    setDetail({ loading: true, campaign, history: [] });
    try {
      const [fresh, history] = await Promise.all([
        adminPromotionService.getCampaign(campaign.publicId),
        adminPromotionService.getApprovalHistory(campaign.publicId).catch(() => []),
      ]);
      setDetail({ loading: false, campaign: fresh, history });
    } catch (error) {
      triggerToast?.(friendlyPromotionError(error), 'error');
      setDetail(null);
    }
  };

  const openBenefitDetail = async (entityType, record) => {
    setFormErrors({});
    setModal({ type: 'benefitDetail', entityType, record, loading: true });
    const loader = {
      coupon: adminPromotionService.getCoupon,
      voucher: adminPromotionService.getVoucher,
      compensation: adminPromotionService.getCompensation,
    }[entityType];
    try {
      const fresh = await loader(record.publicId);
      setModal(current => (
        current?.type === 'benefitDetail' && current.record?.publicId === record.publicId
          ? { ...current, record: fresh, loading: false }
          : current
      ));
    } catch (error) {
      triggerToast?.(friendlyPromotionError(error), 'error');
      setModal(current => current?.type === 'benefitDetail' ? null : current);
    }
  };

  const handleRuleCloneSubmit = (rule, payload, localErrors) => {
    if (localErrors || !payload) {
      setFormErrors(localErrors || {});
      return;
    }
    submitWithFeedback(
      () => adminPromotionService.cloneRule(rule.publicId, payload),
      'Đã nhân bản rule.'
    );
    setDetail(null);
  };

  const transitionCampaign = async (campaign, action) => {
    const dangerous = ['KILL_SWITCH', 'CANCEL', 'PAUSE'].includes(action);
    const comment = dangerous
      ? await triggerPrompt?.({
          title: labelFor(action),
          message: 'Nhập lý do để lưu vào lịch sử phê duyệt/vận hành.',
          label: 'Lý do',
          placeholder: 'Ví dụ: ngân sách đã hết hoặc cấu hình cần rà soát',
          confirmLabel: labelFor(action),
        })
      : undefined;
    if (dangerous && !comment) return;
    await submitWithFeedback(
      () => adminPromotionService.transitionCampaign(campaign.publicId, action, comment),
      `Đã ${labelFor(action).toLowerCase()} chiến dịch.`
    );
    setDetail(null);
  };

  const approveCampaign = async (campaign, accepted) => {
    const comment = await triggerPrompt?.({
      title: accepted ? 'Duyệt chiến dịch' : 'Từ chối chiến dịch',
      label: 'Ghi chú',
      placeholder: accepted ? 'Đã kiểm tra ngân sách và rule.' : 'Nêu rõ lý do cần chỉnh sửa.',
      confirmLabel: accepted ? 'Duyệt' : 'Từ chối',
    });
    if (!comment) return;
    await submitWithFeedback(
      () => accepted
        ? adminPromotionService.approveCampaign(campaign.publicId, comment)
        : adminPromotionService.rejectCampaign(campaign.publicId, comment),
      accepted ? 'Đã duyệt chiến dịch.' : 'Đã từ chối chiến dịch.'
    );
    setDetail(null);
  };

  const legalReview = async campaign => {
    setModal({ type: 'legal', record: campaign });
  };

  const deleteEntity = async (type, record) => {
    const confirmed = await triggerConfirm?.({
      title: 'Xác nhận xóa/vô hiệu hóa',
      message: `Thao tác này sẽ thay đổi trạng thái ${record.name || record.code}.`,
      confirmLabel: 'Tiếp tục',
      tone: 'danger',
    });
    if (!confirmed) return;
    const action = {
      campaign: () => adminPromotionService.deleteCampaign(record.publicId),
      rule: () => adminPromotionService.deleteRule(record.publicId),
      coupon: () => adminPromotionService.disableCoupon(record.publicId),
    }[type];
    await submitWithFeedback(action, 'Đã cập nhật trạng thái.');
    setDetail(null);
  };

  const revokeVoucher = async voucher => {
    const reason = await triggerPrompt?.({
      title: 'Thu hồi voucher',
      label: 'Lý do',
      placeholder: 'Ví dụ: phát hành nhầm khách hàng',
      confirmLabel: 'Thu hồi',
    });
    if (!reason) return;
    await submitWithFeedback(() => adminPromotionService.revokeVoucher(voucher.publicId, reason), 'Đã thu hồi voucher.');
  };

  const extendVoucher = async voucher => {
    setModal({ type: 'extendVoucher', record: voucher });
  };

  const exportCoupons = async () => {
    try {
      const blob = await adminPromotionService.exportCoupons(queries.coupons);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'coupons.csv';
      link.click();
      URL.revokeObjectURL(url);
      triggerToast?.('Đã xuất danh sách coupon.');
    } catch (error) {
      triggerToast?.(friendlyPromotionError(error), 'error');
    }
  };

  const importCoupons = async event => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    await submitWithFeedback(() => adminPromotionService.importCoupons(file), 'Đã import coupon.');
  };

  const headerMetrics = useMemo(() => {
    const campaigns = pages.campaigns.content;
    return [
      { label: 'Chiến dịch đang chạy', value: campaigns.filter(item => item.status === 'ACTIVE').length, icon: CheckCircle2, color: 'text-emerald-300' },
      { label: 'Coupon trên trang', value: pages.coupons.totalElements, icon: TicketPercent, color: 'text-brand-orange' },
      { label: 'Voucher trên trang', value: pages.vouchers.totalElements, icon: WalletCards, color: 'text-sky-300' },
      { label: 'Reservation active', value: pages.reservations.content.filter(item => item.status === 'ACTIVE').length, icon: PauseCircle, color: 'text-amber-300' },
    ];
  }, [pages]);

  return (
    <section className="min-h-full space-y-6 bg-zinc-950 text-white">
      <header className="flex flex-col gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-brand-orange">Promotion</p>
          <h1 className="mt-2 text-3xl font-black tracking-tight">Trung tâm khuyến mãi</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            Quản lý campaign, rule, coupon, voucher, bồi thường và lịch sử sử dụng theo đúng API promotion-service hiện có.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => setModal({ type: 'campaign' })} className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-brand-orange px-4 text-sm font-black text-zinc-950 hover:bg-amber-400">
            <Plus className="h-4 w-4" /> Tạo chiến dịch
          </button>
          <button type="button" onClick={refreshAll} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-zinc-700 px-4 text-sm font-bold text-zinc-300 hover:bg-zinc-800">
            <RefreshCw className="h-4 w-4" /> Làm mới
          </button>
        </div>
      </header>

      <div className="grid gap-3 md:grid-cols-4">
        {headerMetrics.map(metric => {
          const Icon = metric.icon;
          return (
            <div key={metric.label} className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-zinc-500">{metric.label}</span>
                <Icon className={`h-5 w-5 ${metric.color}`} />
              </div>
              <p className={`mt-3 text-2xl font-black ${metric.color}`}>{Number(metric.value || 0).toLocaleString('vi-VN')}</p>
            </div>
          );
        })}
      </div>

      <nav className="flex gap-2 overflow-x-auto border-b border-zinc-800 pb-2">
        {tabs.map(tab => {
          const Icon = tab.icon;
          const active = activeTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={`inline-flex min-h-10 shrink-0 items-center gap-2 rounded-xl px-3 text-sm font-black transition-colors ${active ? 'bg-brand-orange text-zinc-950' : 'border border-zinc-800 bg-zinc-900 text-zinc-400 hover:text-white'}`}
            >
              <Icon className="h-4 w-4" />
              {tab.label}
            </button>
          );
        })}
      </nav>

      <FilterPanel
        activeTab={activeTab}
        query={currentQuery}
        updateQuery={updateQuery}
        campaignOptions={campaignOptions}
        customerOptions={customerOptions}
        onExportCoupons={exportCoupons}
        onImportCoupons={importCoupons}
        onOpenCreate={type => setModal({ type })}
      />

      <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/30">
        {loading ? (
          <div className="flex min-h-72 items-center justify-center gap-3 text-sm font-bold text-zinc-500">
            <Loader2 className="h-5 w-5 animate-spin text-brand-orange" />
            Đang tải dữ liệu khuyến mãi...
          </div>
        ) : currentPage.content.length === 0 ? (
          <div className="flex min-h-72 flex-col items-center justify-center px-4 text-center">
            <Gift className="h-12 w-12 text-zinc-700" />
            <h3 className="mt-4 text-lg font-black text-zinc-200">Chưa có dữ liệu phù hợp</h3>
            <p className="mt-2 max-w-md text-sm text-zinc-500">Thử đổi bộ lọc hoặc tạo mới đối tượng khuyến mãi tương ứng.</p>
          </div>
        ) : (
          <PromotionTable
            activeTab={activeTab}
            rows={currentPage.content}
            onDetail={openCampaignDetail}
            onBenefitDetail={openBenefitDetail}
            onEdit={record => setModal({ type: activeTab.slice(0, -1), record })}
            onCloneRule={record => setModal({ type: 'cloneRule', record })}
            onDelete={deleteEntity}
            onRevokeVoucher={revokeVoucher}
            onExtendVoucher={extendVoucher}
          />
        )}
        <Pagination page={currentPage} onPage={page => updateQuery('page', page)} />
      </section>

      {detail && (
        <CampaignDetailDrawer
          detail={detail}
          onClose={() => setDetail(null)}
          onEdit={campaign => setModal({ type: 'campaign', record: campaign })}
          onDelete={campaign => deleteEntity('campaign', campaign)}
          onCreateRule={campaign => setModal({ type: 'rule', selectedCampaignId: campaign.publicId })}
          onEditRule={rule => setModal({ type: 'rule', record: rule })}
          onDeleteRule={rule => deleteEntity('rule', rule)}
          onCloneRule={rule => setModal({ type: 'cloneRule', record: rule })}
          onTransition={transitionCampaign}
          onApprove={approveCampaign}
          onLegalReview={legalReview}
        />
      )}

      {modal && (
        <PromotionModal
          modal={modal}
          campaignOptions={campaignOptions}
          customerOptions={customerOptions}
          movieOptions={movieOptions}
          cinemaOptions={cinemaOptions}
          tierOptions={tierOptions}
          saving={saving}
          errors={formErrors}
          onClose={closeModal}
          onCampaignSubmit={handleCampaignSubmit}
          onRuleSubmit={handleRuleSubmit}
          onRuleCloneSubmit={handleRuleCloneSubmit}
          onCouponSubmit={handleCouponSubmit}
          onVoucherSubmit={handleVoucherSubmit}
          onCompensationSubmit={handleCompensationSubmit}
          onPreviewRule={adminPromotionService.previewRule}
          onLegalSubmit={async payload => {
            await submitWithFeedback(
              () => adminPromotionService.reviewCampaignLegal(modal.record.publicId, payload),
              'Đã ghi nhận rà soát pháp lý.'
            );
            setDetail(null);
          }}
          onExtendVoucher={async payload => submitWithFeedback(
            () => adminPromotionService.extendVoucher(modal.record.publicId, payload),
            'Đã gia hạn voucher.'
          )}
        />
      )}
    </section>
  );
}

function FilterPanel({ activeTab, query, updateQuery, campaignOptions, customerOptions, onExportCoupons, onImportCoupons, onOpenCreate }) {
  const commonSearch = (
    <Field label="Tìm kiếm">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" />
        <input value={query.keyword || query.name || ''} onChange={event => updateQuery(activeTab === 'campaigns' ? 'name' : 'keyword', event.target.value)} className={`${inputClass} pl-10`} placeholder="Tên, mã hoặc từ khóa" />
      </div>
    </Field>
  );

  return (
    <section className="rounded-2xl border border-zinc-800 bg-zinc-900/50 p-4">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-base font-black text-white">Bộ lọc</h2>
        <div className="flex flex-wrap gap-2">
          {activeTab === 'campaigns' && <button type="button" onClick={() => onOpenCreate('campaign')} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950"><Plus className="h-3.5 w-3.5" /> Chiến dịch</button>}
          {activeTab === 'rules' && <button type="button" onClick={() => onOpenCreate('rule')} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950"><Plus className="h-3.5 w-3.5" /> Rule</button>}
          {activeTab === 'coupons' && (
            <>
              <button type="button" onClick={() => onOpenCreate('coupon')} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950"><Plus className="h-3.5 w-3.5" /> Coupon</button>
              <button type="button" onClick={onExportCoupons} className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300"><Download className="h-3.5 w-3.5" /> Export</button>
              <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300">
                <Upload className="h-3.5 w-3.5" /> Import CSV
                <input type="file" accept=".csv,text/csv" onChange={onImportCoupons} className="sr-only" />
              </label>
            </>
          )}
          {activeTab === 'vouchers' && <button type="button" onClick={() => onOpenCreate('voucher')} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950"><Plus className="h-3.5 w-3.5" /> Voucher</button>}
          {activeTab === 'compensations' && <button type="button" onClick={() => onOpenCreate('compensation')} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950"><Plus className="h-3.5 w-3.5" /> Bồi thường</button>}
        </div>
      </div>
      <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-5">
        {['campaigns', 'coupons', 'vouchers'].includes(activeTab) && commonSearch}
        {activeTab === 'campaigns' && (
          <>
            <Field label="Mã chiến dịch"><input value={query.code} onChange={event => updateQuery('code', event.target.value)} className={inputClass} /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={CAMPAIGN_STATUSES} /></Field>
            <Field label="Từ ngày"><input type="datetime-local" value={query.from} onChange={event => updateQuery('from', event.target.value)} className={inputClass} /></Field>
            <Field label="Đến ngày"><input type="datetime-local" value={query.to} onChange={event => updateQuery('to', event.target.value)} className={inputClass} /></Field>
          </>
        )}
        {activeTab === 'rules' && (
          <>
            <Field label="Chiến dịch"><SearchableSelect options={campaignOptions} value={query.campaignPublicId} onChange={value => updateQuery('campaignPublicId', value)} placeholder="Tất cả campaign" /></Field>
            <Field label="Mã rule"><input value={query.code} onChange={event => updateQuery('code', event.target.value)} className={inputClass} /></Field>
            <Field label="Trạng thái">
              <select value={query.enabled} onChange={event => updateQuery('enabled', event.target.value)} className={inputClass}>
                <option value="">Tất cả</option>
                <option value="true">Đang bật</option>
                <option value="false">Đã tắt</option>
              </select>
            </Field>
          </>
        )}
        {activeTab === 'coupons' && (
          <>
            <Field label="Campaign"><SearchableSelect options={campaignOptions} value={query.campaignPublicId} onChange={value => updateQuery('campaignPublicId', value)} placeholder="Tất cả campaign" /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={COUPON_STATUSES} /></Field>
            <Field label="Hiệu lực tại"><input type="datetime-local" value={query.validAt} onChange={event => updateQuery('validAt', event.target.value)} className={inputClass} /></Field>
          </>
        )}
        {activeTab === 'vouchers' && (
          <>
            <Field label="Campaign"><SearchableSelect options={campaignOptions} value={query.campaignPublicId} onChange={value => updateQuery('campaignPublicId', value)} placeholder="Tất cả campaign" /></Field>
            <Field label="Khách hàng"><SearchableSelect options={customerOptions} value={query.ownerPublicId} onChange={value => updateQuery('ownerPublicId', value)} placeholder="Tất cả khách hàng" /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={VOUCHER_STATUSES} /></Field>
            <Field label="Nguồn"><SelectField value={query.source} onChange={value => updateQuery('source', value)} options={VOUCHER_SOURCES} /></Field>
          </>
        )}
        {activeTab === 'compensations' && (
          <>
            <Field label="Khách hàng"><SearchableSelect options={customerOptions} value={query.userPublicId} onChange={value => updateQuery('userPublicId', value)} placeholder="Tất cả khách hàng" /></Field>
            <Field label="Loại"><SelectField value={query.type} onChange={value => updateQuery('type', value)} options={COMPENSATION_TYPES} /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={COMPENSATION_STATUSES} /></Field>
            <Field label="Từ ngày"><input type="datetime-local" value={query.from} onChange={event => updateQuery('from', event.target.value)} className={inputClass} /></Field>
            <Field label="Đến ngày"><input type="datetime-local" value={query.to} onChange={event => updateQuery('to', event.target.value)} className={inputClass} /></Field>
          </>
        )}
        {activeTab === 'redemptions' && (
          <>
            <Field label="Loại"><SelectField value={query.type} onChange={value => updateQuery('type', value)} options={REDEMPTION_TYPES} /></Field>
            <Field label="Khách hàng"><input value={query.userPublicId} onChange={event => updateQuery('userPublicId', event.target.value)} className={inputClass} /></Field>
            <Field label="Booking"><input value={query.bookingPublicId} onChange={event => updateQuery('bookingPublicId', event.target.value)} className={inputClass} /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={REDEMPTION_STATUSES} /></Field>
            <Field label="Từ ngày"><input type="datetime-local" value={query.from} onChange={event => updateQuery('from', event.target.value)} className={inputClass} /></Field>
          </>
        )}
        {activeTab === 'reservations' && (
          <>
            <Field label="Loại"><SelectField value={query.type} onChange={value => updateQuery('type', value)} options={REDEMPTION_TYPES} /></Field>
            <Field label="Trạng thái"><SelectField value={query.status} onChange={value => updateQuery('status', value)} options={RESERVATION_STATUSES} /></Field>
            <Field label="Khách hàng"><input value={query.userPublicId} onChange={event => updateQuery('userPublicId', event.target.value)} className={inputClass} /></Field>
            <Field label="Booking"><input value={query.bookingPublicId} onChange={event => updateQuery('bookingPublicId', event.target.value)} className={inputClass} /></Field>
            <Field label="Order"><input value={query.orderPublicId} onChange={event => updateQuery('orderPublicId', event.target.value)} className={inputClass} /></Field>
          </>
        )}
        {query.sort && (
          <Field label="Sắp xếp">
            <select value={query.sort} onChange={event => updateQuery('sort', event.target.value)} className={inputClass}>
              {(sortOptionsByTab[activeTab] || []).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </Field>
        )}
      </div>
    </section>
  );
}

function PromotionTable({ activeTab, rows, onDetail, onBenefitDetail, onEdit, onCloneRule, onDelete, onRevokeVoucher, onExtendVoucher }) {
  if (activeTab === 'campaigns') return <CampaignTable rows={rows} onDetail={onDetail} onEdit={onEdit} onDelete={onDelete} />;
  if (activeTab === 'rules') return <RuleTable rows={rows} onEdit={onEdit} onClone={onCloneRule} onDelete={onDelete} />;
  if (activeTab === 'coupons') return <CouponTable rows={rows} onDetail={row => onBenefitDetail('coupon', row)} onEdit={onEdit} onDelete={onDelete} />;
  if (activeTab === 'vouchers') return <VoucherTable rows={rows} onDetail={row => onBenefitDetail('voucher', row)} onEdit={onEdit} onRevoke={onRevokeVoucher} onExtend={onExtendVoucher} />;
  if (activeTab === 'compensations') return <CompensationTable rows={rows} onDetail={row => onBenefitDetail('compensation', row)} onEdit={onEdit} />;
  if (activeTab === 'redemptions') return <RedemptionTable rows={rows} />;
  return <ReservationTable rows={rows} />;
}

function RuleTable({ rows, onEdit, onClone, onDelete }) {
  return (
    <DataTable headers={['Rule', 'Chiến dịch', 'Điều kiện', 'Thứ tự', 'Trạng thái', 'Thao tác']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-black text-white">{row.name}</p><p className="mt-1 font-mono text-xs text-zinc-500">{row.code} · {labelFor(row.ruleType)}</p></td>
          <td className="px-4 py-4 font-mono text-xs text-zinc-500">{row.campaignPublicId}</td>
          <td className="max-w-xs px-4 py-4 text-xs leading-5 text-zinc-400">{conditionSummary(row.conditionsJson)}<br /><span className="font-bold text-emerald-300">{voucherDiscountSummary(row)}</span></td>
          <td className="px-4 py-4 text-xs text-zinc-300">Ưu tiên {row.priority}<br /><span className="text-zinc-500">Chạy thứ {row.executionOrder}</span></td>
          <td className="px-4 py-4"><Badge status={row.enabled ? 'ACTIVE' : 'DISABLED'} /></td>
          <td className="px-4 py-4"><div className="flex gap-2"><IconButton label="Sửa rule" onClick={() => onEdit(row)} icon={FileSpreadsheet} /><IconButton label="Nhân bản rule" onClick={() => onClone(row)} icon={Copy} /><IconButton label="Xóa rule" tone="danger" onClick={() => onDelete('rule', row)} icon={Trash2} /></div></td>
        </tr>
      ))}
    </DataTable>
  );
}

function DataTable({ headers, children }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full text-left text-sm">
        <thead className="border-b border-zinc-800 bg-zinc-950/60 text-xs font-black uppercase tracking-wider text-zinc-500">
          <tr>{headers.map(header => <th key={header} className="px-4 py-3">{header}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-zinc-800/60">{children}</tbody>
      </table>
    </div>
  );
}

function CampaignTable({ rows, onDetail, onEdit, onDelete }) {
  return (
    <DataTable headers={['Chiến dịch', 'Trạng thái', 'Thời gian', 'Ngân sách', 'Lượt dùng', 'Thao tác']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4">
            <p className="font-black text-white">{row.name}</p>
            <p className="mt-1 text-xs text-zinc-500">{row.code} · {labelFor(row.campaignType)}</p>
          </td>
          <td className="px-4 py-4"><div className="flex flex-col gap-1"><Badge status={row.status} /><Badge status={row.approvalStatus} /></div></td>
          <td className="px-4 py-4 text-xs text-zinc-400">{formatDateTime(row.startAt)}<br />{formatDateTime(row.endAt)}</td>
          <td className="px-4 py-4 text-xs text-zinc-300">{currency(row.budgetUsed)} / {currency(row.budgetAmount)}<br /><span className="text-zinc-500">Còn {currency(row.budgetRemaining)}</span></td>
          <td className="px-4 py-4 text-xs text-zinc-300">{row.redemptionCount || 0}{row.maxRedemptions ? ` / ${row.maxRedemptions}` : ''}<br /><span className="text-zinc-500">Mỗi khách {row.maxRedemptionsPerUser}</span></td>
          <td className="px-4 py-4">
            <div className="flex flex-wrap gap-2">
              <IconButton label="Chi tiết" onClick={() => onDetail(row)} icon={Eye} />
              <IconButton label="Sửa" onClick={() => onEdit(row)} icon={FileSpreadsheet} />
              <IconButton label="Xóa" tone="danger" onClick={() => onDelete('campaign', row)} icon={Trash2} />
            </div>
          </td>
        </tr>
      ))}
    </DataTable>
  );
}

function CouponTable({ rows, onDetail, onEdit, onDelete }) {
  return (
    <DataTable headers={['Coupon', 'Giá trị', 'Điều kiện', 'Hiệu lực', 'Lượt dùng', 'Thao tác']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-black text-white">{row.name}</p><p className="mt-1 font-mono text-xs text-zinc-500">{row.code}</p><div className="mt-2"><Badge status={row.status} /></div></td>
          <td className="px-4 py-4 text-xs font-bold text-emerald-300">{voucherDiscountSummary(row)}</td>
          <td className="max-w-xs px-4 py-4 text-xs leading-5 text-zinc-400">{conditionSummary(row.conditionsJson)}</td>
          <td className="px-4 py-4 text-xs text-zinc-400">{formatDateTime(row.validFrom)}<br />{formatDateTime(row.validTo)}</td>
          <td className="px-4 py-4 text-xs text-zinc-300">{row.redemptionCount || 0}{row.maxRedemptions ? ` / ${row.maxRedemptions}` : ''}<br /><span className="text-zinc-500">Mỗi khách {row.maxRedemptionsPerUser}</span></td>
          <td className="px-4 py-4"><div className="flex flex-wrap gap-2"><IconButton label="Chi tiết" onClick={() => onDetail(row)} icon={Eye} /><IconButton label="Sửa" onClick={() => onEdit(row)} icon={FileSpreadsheet} /><IconButton label="Vô hiệu hóa" tone="danger" onClick={() => onDelete('coupon', row)} icon={Trash2} /></div></td>
        </tr>
      ))}
    </DataTable>
  );
}

function VoucherTable({ rows, onDetail, onEdit, onRevoke, onExtend }) {
  return (
    <DataTable headers={['Voucher', 'Khách hàng', 'Giá trị', 'Hiệu lực', 'Sử dụng', 'Thao tác']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-black text-white">{row.name}</p><p className="mt-1 font-mono text-xs text-zinc-500">{row.code}</p><div className="mt-2"><Badge status={row.status} /></div></td>
          <td className="px-4 py-4 text-xs text-zinc-300">{row.ownerPublicId}<br /><span className="text-zinc-500">{labelFor(row.source)}</span></td>
          <td className="px-4 py-4 text-xs font-bold text-emerald-300">{voucherDiscountSummary(row)}<br /><span className="text-zinc-500">{row.minimumOrderAmount ? `Đơn từ ${currency(row.minimumOrderAmount)}` : 'Không có tối thiểu riêng'}</span></td>
          <td className="px-4 py-4 text-xs text-zinc-400">{formatDateTime(row.validFrom)}<br />{formatDateTime(row.validTo)}</td>
          <td className="px-4 py-4 text-xs text-zinc-300">{row.usageCount || 0} / {row.maxUsage}</td>
          <td className="px-4 py-4"><div className="flex flex-wrap gap-2"><IconButton label="Chi tiết" onClick={() => onDetail(row)} icon={Eye} /><IconButton label="Sửa" onClick={() => onEdit(row)} icon={FileSpreadsheet} /><IconButton label="Gia hạn" onClick={() => onExtend(row)} icon={CalendarClock} /><IconButton label="Thu hồi" tone="danger" onClick={() => onRevoke(row)} icon={Trash2} /></div></td>
        </tr>
      ))}
    </DataTable>
  );
}

function CompensationTable({ rows, onDetail, onEdit }) {
  return (
    <DataTable headers={['Bồi thường', 'Khách hàng', 'Số tiền', 'Liên kết', 'Trạng thái', 'Thao tác']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-black text-white">{labelFor(row.compensationType)}</p><p className="mt-1 max-w-xs text-xs text-zinc-500">{row.reason}</p></td>
          <td className="px-4 py-4 text-xs text-zinc-300">{row.userPublicId}</td>
          <td className="px-4 py-4 text-sm font-black text-emerald-300">{currency(row.amount)}</td>
          <td className="px-4 py-4 text-xs text-zinc-500">Booking: {row.bookingPublicId || '—'}<br />Voucher: {row.voucherPublicId || '—'}</td>
          <td className="px-4 py-4"><Badge status={row.status} /></td>
          <td className="px-4 py-4"><div className="flex gap-2"><IconButton label="Chi tiết" onClick={() => onDetail(row)} icon={Eye} /><IconButton label="Sửa" onClick={() => onEdit(row)} icon={FileSpreadsheet} /></div></td>
        </tr>
      ))}
    </DataTable>
  );
}

function RedemptionTable({ rows }) {
  return (
    <DataTable headers={['Mã', 'Loại', 'Booking/Payment', 'Số tiền', 'Trạng thái', 'Thời gian']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-mono text-xs text-zinc-300">{row.code}</p><p className="mt-1 text-xs text-zinc-500">{row.benefitPublicId}</p></td>
          <td className="px-4 py-4"><Badge status={row.redemptionType} /></td>
          <td className="px-4 py-4 text-xs text-zinc-500">Booking: {row.bookingPublicId || '—'}<br />Payment: {row.paymentPublicId || '—'}</td>
          <td className="px-4 py-4 text-xs text-zinc-300">{currency(row.originalAmount)}<br /><span className="text-emerald-300">-{currency(row.discountAmount)}</span><br /><span className="font-black text-white">{currency(row.finalAmount)}</span></td>
          <td className="px-4 py-4"><Badge status={row.status} /></td>
          <td className="px-4 py-4 text-xs text-zinc-400">{formatDateTime(row.confirmedAt || row.createdAt)}</td>
        </tr>
      ))}
    </DataTable>
  );
}

function ReservationTable({ rows }) {
  return (
    <DataTable headers={['Reservation', 'Loại', 'Khách/Booking', 'Số tiền', 'Trạng thái', 'Thời hạn']}>
      {rows.map(row => (
        <tr key={row.publicId} className="hover:bg-zinc-900/70">
          <td className="px-4 py-4"><p className="font-mono text-xs text-zinc-300">{row.reservationCode}</p><p className="mt-1 text-xs text-zinc-500">{row.publicId}</p></td>
          <td className="px-4 py-4"><Badge status={row.reservationType} /></td>
          <td className="px-4 py-4 text-xs text-zinc-500">User: {row.userPublicId || '—'}<br />Booking: {row.bookingPublicId || '—'}</td>
          <td className="px-4 py-4 text-xs text-zinc-300">{currency(row.originalAmount)}<br /><span className="text-emerald-300">-{currency(row.discountAmount)}</span><br /><span className="font-black text-white">{currency(row.finalAmount)}</span></td>
          <td className="px-4 py-4"><Badge status={row.status} /></td>
          <td className="px-4 py-4 text-xs text-zinc-400">{formatDateTime(row.reservationStartedAt)}<br />{formatDateTime(row.reservationExpiredAt)}</td>
        </tr>
      ))}
    </DataTable>
  );
}

function IconButton({ label, icon: Icon, onClick, tone = 'default' }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={label}
      aria-label={label}
      className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border transition-colors ${tone === 'danger' ? 'border-red-500/30 text-red-300 hover:bg-red-500/10' : 'border-zinc-700 text-zinc-300 hover:bg-zinc-800'}`}
    >
      <Icon className="h-4 w-4" />
    </button>
  );
}

function CampaignDetailDrawer({ detail, onClose, onEdit, onDelete, onCreateRule, onEditRule, onDeleteRule, onCloneRule, onTransition, onApprove, onLegalReview }) {
  const campaign = detail.campaign;
  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/70 backdrop-blur-sm">
      <section className="h-full w-full max-w-4xl overflow-y-auto border-l border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        <header className="flex items-start justify-between gap-4 border-b border-zinc-800 pb-5">
          <div>
            <p className="text-xs font-black uppercase tracking-widest text-brand-orange">Chi tiết campaign</p>
            <h2 className="mt-2 text-2xl font-black text-white">{campaign?.name}</h2>
            <p className="mt-1 text-sm text-zinc-500">{campaign?.code} · {labelFor(campaign?.campaignType)}</p>
          </div>
          <button type="button" onClick={onClose} className="rounded-xl p-2 text-zinc-500 hover:bg-zinc-800 hover:text-white" aria-label="Đóng chi tiết"><X className="h-5 w-5" /></button>
        </header>
        {detail.loading ? (
          <div className="flex min-h-80 items-center justify-center gap-3 text-zinc-500"><Loader2 className="h-5 w-5 animate-spin text-brand-orange" />Đang tải chi tiết...</div>
        ) : (
          <div className="space-y-6 py-5">
            <div className="grid gap-3 md:grid-cols-4">
              <InfoCard label="Trạng thái" value={<Badge status={campaign.status} />} />
              <InfoCard label="Duyệt" value={<Badge status={campaign.approvalStatus} />} />
              <InfoCard label="Pháp lý" value={<Badge status={campaign.legalStatus} />} />
              <InfoCard label="Ngân sách còn" value={currency(campaign.budgetRemaining)} />
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" onClick={() => onEdit(campaign)} className="rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800">Sửa campaign</button>
              <button type="button" onClick={() => onCreateRule(campaign)} className="rounded-xl bg-brand-orange px-3 py-2 text-xs font-black text-zinc-950">Thêm rule</button>
              {availableCampaignTransitions(campaign).map(action => (
                <button key={action} type="button" onClick={() => onTransition(campaign, action)} className="rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold text-zinc-300 hover:bg-zinc-800">{labelFor(action)}</button>
              ))}
              {campaign.approvalStatus === 'PENDING' && (
                <>
                  <button type="button" onClick={() => onApprove(campaign, true)} className="rounded-xl border border-emerald-500/30 px-3 py-2 text-xs font-bold text-emerald-300 hover:bg-emerald-500/10">Duyệt</button>
                  <button type="button" onClick={() => onApprove(campaign, false)} className="rounded-xl border border-red-500/30 px-3 py-2 text-xs font-bold text-red-300 hover:bg-red-500/10">Từ chối</button>
                </>
              )}
              <button type="button" onClick={() => onLegalReview(campaign)} className="rounded-xl border border-sky-500/30 px-3 py-2 text-xs font-bold text-sky-300 hover:bg-sky-500/10">Rà soát pháp lý</button>
              <button type="button" onClick={() => onDelete(campaign)} className="rounded-xl border border-red-500/30 px-3 py-2 text-xs font-bold text-red-300 hover:bg-red-500/10">Xóa mềm</button>
            </div>
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4">
              <h3 className="text-base font-black text-white">Rules trong campaign</h3>
              <div className="mt-4 divide-y divide-zinc-800">
                {(campaign.rules || []).length === 0 ? <p className="py-8 text-center text-sm text-zinc-500">Chưa có rule nào.</p> : campaign.rules.map(rule => (
                  <div key={rule.publicId} className="grid gap-3 py-4 md:grid-cols-[minmax(0,1fr)_180px_120px] md:items-center">
                    <div>
                      <p className="font-black text-white">{rule.name}</p>
                      <p className="mt-1 text-xs text-zinc-500">{rule.code} · {labelFor(rule.ruleType)} · {conditionSummary(rule.conditionsJson)}</p>
                    </div>
                    <div><Badge status={rule.enabled ? 'ACTIVE' : 'DISABLED'} /></div>
                    <div className="flex gap-2 md:justify-end">
                      <IconButton label="Sửa rule" onClick={() => onEditRule(rule)} icon={FileSpreadsheet} />
                      <IconButton label="Nhân bản rule" onClick={() => onCloneRule(rule)} icon={Copy} />
                      <IconButton label="Xóa rule" tone="danger" onClick={() => onDeleteRule(rule)} icon={Trash2} />
                    </div>
                  </div>
                ))}
              </div>
            </section>
            <section className="rounded-2xl border border-zinc-800 bg-zinc-900/40 p-4">
              <h3 className="text-base font-black text-white">Lịch sử duyệt</h3>
              <div className="mt-4 space-y-3">
                {(detail.history || []).length === 0 ? <p className="py-8 text-center text-sm text-zinc-500">Chưa có lịch sử.</p> : detail.history.map(item => (
                  <div key={item.publicId} className="rounded-xl border border-zinc-800 bg-zinc-950 p-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <Badge status={item.action} />
                      <span className="text-xs text-zinc-500">{formatDateTime(item.approvedAt || item.createdAt)}</span>
                    </div>
                    <p className="mt-2 text-sm text-zinc-300">{item.comment || 'Không có ghi chú.'}</p>
                    <p className="mt-1 text-xs text-zinc-600">{item.oldStatus || '—'} → {item.newStatus || '—'} · {item.approverPublicId || item.createdBy || 'SYSTEM'}</p>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}
      </section>
    </div>
  );
}

function InfoCard({ label, value }) {
  return (
    <div className="rounded-2xl border border-zinc-800 bg-zinc-900/60 p-4">
      <p className="text-xs font-bold text-zinc-500">{label}</p>
      <div className="mt-2 text-sm font-black text-white">{value}</div>
    </div>
  );
}

function PromotionModal({
  modal,
  campaignOptions,
  customerOptions,
  movieOptions,
  cinemaOptions,
  tierOptions,
  saving,
  errors,
  onClose,
  onCampaignSubmit,
  onRuleSubmit,
  onRuleCloneSubmit,
  onCouponSubmit,
  onVoucherSubmit,
  onCompensationSubmit,
  onPreviewRule,
  onLegalSubmit,
  onExtendVoucher,
}) {
  if (modal.type === 'benefitDetail') {
    return <BenefitDetailModal modal={modal} onClose={onClose} />;
  }
  if (modal.type === 'cloneRule') {
    return (
      <RuleCloneModal
        rule={modal.record}
        campaignOptions={campaignOptions}
        saving={saving}
        errors={errors}
        onSubmit={(payload, localErrors) => onRuleCloneSubmit(modal.record, payload, localErrors)}
        onClose={onClose}
      />
    );
  }
  if (modal.type === 'campaign') {
    return (
      <Modal title={modal.record ? 'Chỉnh sửa chiến dịch' : 'Tạo chiến dịch'} subtitle="Thiết lập thời gian, ngân sách, trạng thái duyệt và giới hạn sử dụng." onClose={onClose} wide>
        <CampaignForm record={modal.record} onSubmit={onCampaignSubmit} onCancel={onClose} saving={saving} errors={errors} />
      </Modal>
    );
  }
  if (modal.type === 'rule') {
    return (
      <Modal title={modal.record ? 'Chỉnh sửa rule' : 'Tạo rule'} subtitle="Thiết lập đối tượng, điều kiện áp dụng và giá trị giảm; có thể xem trước kết quả từ backend." onClose={onClose} wide>
        <RuleForm record={modal.record} selectedCampaignId={modal.selectedCampaignId} campaignOptions={campaignOptions} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} onSubmit={onRuleSubmit} onCancel={onClose} onPreview={onPreviewRule} saving={saving} errors={errors} />
      </Modal>
    );
  }
  if (modal.type === 'coupon') {
    return (
      <Modal title={modal.record ? 'Chỉnh sửa coupon' : 'Tạo coupon'} subtitle="Tạo một mã hoặc sinh hàng loạt theo campaign, có import/export CSV từ backend." onClose={onClose} wide>
        <CouponForm record={modal.record} campaignOptions={campaignOptions} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} onSubmit={onCouponSubmit} onCancel={onClose} saving={saving} errors={errors} />
      </Modal>
    );
  }
  if (modal.type === 'voucher') {
    return (
      <Modal title={modal.record ? 'Chỉnh sửa voucher' : 'Phát hành voucher'} subtitle="Chọn khách hàng từ user-service hoặc nhập account ID/UUID khi cần." onClose={onClose} wide>
        <VoucherForm record={modal.record} campaignOptions={campaignOptions} customerOptions={customerOptions} movieOptions={movieOptions} cinemaOptions={cinemaOptions} tierOptions={tierOptions} onSubmit={onVoucherSubmit} onCancel={onClose} saving={saving} errors={errors} />
      </Modal>
    );
  }
  if (modal.type === 'compensation') {
    return (
      <Modal title={modal.record ? 'Cập nhật bồi thường' : 'Phát hành bồi thường'} subtitle="Phát hành voucher bồi thường có audit history theo API promotion-service." onClose={onClose}>
        <CompensationForm record={modal.record} customerOptions={customerOptions} onSubmit={onCompensationSubmit} onCancel={onClose} saving={saving} errors={errors} />
      </Modal>
    );
  }
  if (modal.type === 'legal') {
    return <LegalReviewModal campaign={modal.record} saving={saving} errors={errors} onSubmit={onLegalSubmit} onClose={onClose} />;
  }
  if (modal.type === 'extendVoucher') {
    return <ExtendVoucherModal voucher={modal.record} saving={saving} errors={errors} onSubmit={onExtendVoucher} onClose={onClose} />;
  }
  return null;
}

function RuleCloneModal({ rule, campaignOptions, saving, errors, onSubmit, onClose }) {
  const [form, setForm] = useState({
    newCode: `${rule.code}_COPY`.slice(0, 100),
    newName: `Bản sao ${rule.name}`.slice(0, 255),
    targetCampaignPublicId: rule.campaignPublicId || '',
  });
  const submit = event => {
    event.preventDefault();
    const localErrors = {};
    if (!form.newCode.trim()) localErrors.newCode = requiredMessage;
    if (!/^[A-Za-z0-9_-]+$/.test(form.newCode)) localErrors.newCode = 'Mã chỉ gồm chữ, số, gạch dưới hoặc gạch nối.';
    if (!form.newName.trim()) localErrors.newName = requiredMessage;
    if (hasErrors(localErrors)) {
      onSubmit(null, localErrors);
      return;
    }
    onSubmit({
      newCode: form.newCode,
      newName: form.newName,
      targetCampaignPublicId: form.targetCampaignPublicId || null,
    });
  };

  return (
    <Modal title="Nhân bản rule" subtitle={`${rule.name} · ${rule.code}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <Field label="Mã rule mới" required error={errors.newCode}>
          <input value={form.newCode} maxLength={100} onChange={event => setForm(current => ({ ...current, newCode: event.target.value.toUpperCase() }))} className={inputClass} />
        </Field>
        <Field label="Tên rule mới" required error={errors.newName}>
          <input value={form.newName} maxLength={255} onChange={event => setForm(current => ({ ...current, newName: event.target.value }))} className={inputClass} />
        </Field>
        <Field label="Chiến dịch đích" hint="Giữ nguyên để nhân bản trong chiến dịch hiện tại.">
          <SearchableSelect options={campaignOptions} value={form.targetCampaignPublicId} onChange={value => setForm(current => ({ ...current, targetCampaignPublicId: value }))} placeholder="Chọn chiến dịch" />
        </Field>
        <FormActions saving={saving} onCancel={onClose} submitLabel="Nhân bản rule" />
      </form>
    </Modal>
  );
}

function BenefitDetailModal({ modal, onClose }) {
  const record = modal.record || {};
  const labels = {
    coupon: 'Chi tiết coupon',
    voucher: 'Chi tiết voucher',
    compensation: 'Chi tiết bồi thường',
  };
  const type = record.couponType || record.voucherType || record.compensationType;
  const usage = modal.entityType === 'coupon'
    ? `${record.redemptionCount || 0}${record.maxRedemptions ? ` / ${record.maxRedemptions}` : ''}`
    : modal.entityType === 'voucher'
      ? `${record.usageCount || 0} / ${record.maxUsage || 1}`
      : 'Theo voucher đã phát hành';
  const approvalHistory = Array.isArray(record.approvalHistory) ? record.approvalHistory : [];

  return (
    <Modal title={labels[modal.entityType]} subtitle={record.name || record.reason || record.code} onClose={onClose} wide>
      {modal.loading ? (
        <div className="flex min-h-64 items-center justify-center gap-2 text-sm font-bold text-zinc-500">
          <Loader2 className="h-5 w-5 animate-spin text-brand-orange" /> Đang tải chi tiết...
        </div>
      ) : (
        <div className="space-y-5">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <InfoCard label="Trạng thái" value={<Badge status={record.status} />} />
            <InfoCard label="Loại" value={labelFor(type)} />
            <InfoCard label="Giá trị" value={modal.entityType === 'compensation' ? currency(record.amount) : voucherDiscountSummary(record)} />
            <InfoCard label="Lượt sử dụng" value={usage} />
          </div>
          <div className="grid gap-x-8 gap-y-4 border-y border-zinc-800 py-5 text-sm md:grid-cols-2">
            <DetailLine label="Mã" value={record.code || record.voucher?.code} mono />
            <DetailLine label="Khách hàng" value={record.ownerPublicId || record.userPublicId} mono />
            <DetailLine label="Chiến dịch" value={record.campaignPublicId} mono />
            <DetailLine label="Nguồn" value={labelFor(record.source)} />
            <DetailLine label="Hiệu lực từ" value={formatDateTime(record.validFrom || record.issuedAt)} />
            <DetailLine label="Hiệu lực đến" value={formatDateTime(record.validTo || record.expiredAt)} />
            <DetailLine label="Booking" value={record.bookingPublicId} mono />
            <DetailLine label="Reservation" value={record.reservationPublicId} mono />
          </div>
          <div>
            <h3 className="text-sm font-black text-white">Mô tả và điều kiện</h3>
            <p className="mt-2 text-sm leading-6 text-zinc-400">{record.description || record.reason || 'Không có mô tả.'}</p>
            {record.conditionsJson && <p className="mt-2 text-xs leading-5 text-zinc-500">{conditionSummary(record.conditionsJson)}</p>}
          </div>
          {approvalHistory.length > 0 && (
            <div>
              <h3 className="text-sm font-black text-white">Lịch sử duyệt</h3>
              <div className="mt-3 divide-y divide-zinc-800 border-y border-zinc-800">
                {approvalHistory.map((item, index) => (
                  <div key={item.publicId || `${item.action}-${index}`} className="flex flex-wrap items-start justify-between gap-3 py-3 text-xs">
                    <div><Badge status={item.action || item.status} /><p className="mt-2 text-zinc-400">{item.comment || 'Không có ghi chú.'}</p></div>
                    <span className="text-zinc-600">{formatDateTime(item.createdAt || item.approvedAt)}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}

function DetailLine({ label, value, mono = false }) {
  if (!value) return null;
  return (
    <div className="min-w-0">
      <p className="text-xs font-bold text-zinc-600">{label}</p>
      <p className={`mt-1 break-all font-bold text-zinc-300 ${mono ? 'font-mono text-xs' : ''}`}>{value}</p>
    </div>
  );
}

function LegalReviewModal({ campaign, saving, errors, onSubmit, onClose }) {
  const [form, setForm] = useState({ status: campaign.legalStatus || 'PENDING', comment: '', legalNotificationRef: campaign.legalNotificationRef || '' });
  return (
    <Modal title="Rà soát pháp lý" subtitle={campaign.name} onClose={onClose}>
      <form onSubmit={event => { event.preventDefault(); onSubmit(form); }} className="space-y-4">
        <Field label="Kết quả" required error={errors.status}><SelectField value={form.status} onChange={value => setForm(current => ({ ...current, status: value }))} options={LEGAL_STATUSES} placeholder="Chọn kết quả" /></Field>
        <Field label="Ghi chú" required error={errors.comment}><textarea value={form.comment} onChange={event => setForm(current => ({ ...current, comment: event.target.value }))} className={areaClass} /></Field>
        <Field label="Mã thông báo pháp lý" error={errors.legalNotificationRef}><input value={form.legalNotificationRef} onChange={event => setForm(current => ({ ...current, legalNotificationRef: event.target.value }))} className={inputClass} /></Field>
        <FormActions saving={saving} onCancel={onClose} submitLabel="Lưu rà soát" />
      </form>
    </Modal>
  );
}

function ExtendVoucherModal({ voucher, saving, errors, onSubmit, onClose }) {
  const [form, setForm] = useState({ validTo: toDateTimeLocal(voucher.validTo) || nextMonthLocal(), reason: '' });
  return (
    <Modal title="Gia hạn voucher" subtitle={voucher.name} onClose={onClose}>
      <form onSubmit={event => { event.preventDefault(); onSubmit({ validTo: fromDateTimeLocal(form.validTo), reason: form.reason }); }} className="space-y-4">
        <Field label="Hạn mới" required error={errors.validTo}><input type="datetime-local" value={form.validTo} onChange={event => setForm(current => ({ ...current, validTo: event.target.value }))} className={inputClass} /></Field>
        <Field label="Lý do" required error={errors.reason}><textarea value={form.reason} onChange={event => setForm(current => ({ ...current, reason: event.target.value }))} className={areaClass} /></Field>
        <FormActions saving={saving} onCancel={onClose} submitLabel="Gia hạn" />
      </form>
    </Modal>
  );
}
