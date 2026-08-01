import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  BadgePercent,
  CalendarClock,
  Check,
  ChevronLeft,
  ChevronRight,
  CirclePause,
  Copy,
  Edit3,
  Gift,
  Loader2,
  Play,
  Plus,
  RefreshCw,
  Search,
  Send,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import adminPromotionService from '../services/adminPromotionService';

const emptyPage = { content: [], page: 0, size: 12, totalElements: 0, totalPages: 0, last: true };
const fieldClass = 'h-10 w-full rounded-lg border border-zinc-700 bg-zinc-950 px-3 text-sm text-white outline-none focus:border-orange-500';
const buttonClass = 'inline-flex h-9 items-center justify-center gap-2 rounded-lg px-3 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:opacity-40';

const labels = {
  DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối',
  SCHEDULED: 'Đã lên lịch', ACTIVE: 'Đang chạy', PAUSED: 'Tạm dừng', COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy', DISABLED: 'Đã tắt', EXPIRED: 'Hết hạn', PASSED: 'Đạt pháp lý', FAILED: 'Không đạt',
  AUTO: 'Tự động', VOUCHER: 'Voucher', COUPON: 'Coupon',
};

const badge = status => {
  if (['ACTIVE', 'APPROVED', 'PASSED'].includes(status)) return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-300';
  if (['PAUSED', 'PENDING', 'SCHEDULED'].includes(status)) return 'border-amber-500/30 bg-amber-500/10 text-amber-300';
  if (['REJECTED', 'FAILED', 'CANCELLED', 'DISABLED', 'EXPIRED'].includes(status)) return 'border-red-500/30 bg-red-500/10 text-red-300';
  return 'border-zinc-700 bg-zinc-800 text-zinc-300';
};

const money = value => `${Number(value || 0).toLocaleString('vi-VN')}đ`;
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '-';
const toLocalInput = value => {
  const date = value ? new Date(value) : new Date();
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};
const fromLocalInput = value => value ? new Date(value).toISOString() : null;
const pageData = payload => payload?.data ?? payload ?? emptyPage;
const errorText = error => error?.response?.data?.message || error?.message || 'Không thể hoàn tất thao tác.';

function StatusBadge({ value }) {
  return <span className={`inline-flex rounded border px-2 py-1 text-[10px] font-bold ${badge(value)}`}>{labels[value] || value}</span>;
}

function IconButton({ title, onClick, children, danger = false, disabled = false }) {
  return (
    <button type="button" title={title} aria-label={title} onClick={onClick} disabled={disabled}
      className={`inline-flex h-8 w-8 items-center justify-center rounded-lg border transition-colors ${danger ? 'border-red-500/20 text-red-400 hover:bg-red-500/10' : 'border-zinc-700 text-zinc-400 hover:bg-zinc-800 hover:text-white'} disabled:opacity-30`}>
      {children}
    </button>
  );
}

export default function AdminPromotionCenterPage() {
  const [tab, setTab] = useState('campaigns');
  const [campaigns, setCampaigns] = useState(emptyPage);
  const [promotions, setPromotions] = useState(emptyPage);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [type, setType] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState(null);
  const [modal, setModal] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      if (tab === 'campaigns') {
        setCampaigns(pageData(await adminPromotionService.searchCampaigns({
          name: query || undefined, status: status || undefined, page, size: 12, sort: 'createdAt,desc'
        })));
      } else {
        setPromotions(pageData(await adminPromotionService.searchPromotions({
          keyword: query || undefined, status: status || undefined, type: type || undefined,
          page, size: 12, sort: 'createdAt,desc'
        })));
      }
    } catch (error) {
      setMessage({ kind: 'error', text: errorText(error) });
    } finally {
      setLoading(false);
    }
  }, [page, query, status, tab, type]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setPage(0); }, [query, status, tab, type]);

  const campaignOptions = useMemo(() => campaigns.content.map(item => ({ value: item.publicId, label: `${item.name} (${item.code})`, item })), [campaigns]);
  const current = tab === 'campaigns' ? campaigns : promotions;

  const run = async (action, success) => {
    setBusy(true);
    try {
      await action();
      setModal(null);
      setMessage({ kind: 'success', text: success });
      await load();
    } catch (error) {
      setMessage({ kind: 'error', text: errorText(error) });
    } finally {
      setBusy(false);
    }
  };

  const campaignAction = (campaign, action) => run(
    () => action === 'APPROVE'
      ? adminPromotionService.approveCampaign(campaign.publicId, 'Approved from Promotion Center')
      : action === 'LEGAL'
        ? adminPromotionService.reviewCampaignLegal(campaign.publicId, 'PASSED', 'Legal review passed')
        : adminPromotionService.transitionCampaign(campaign.publicId, action, action === 'KILL_SWITCH' ? 'Stopped by operator' : undefined),
    'Đã cập nhật vòng đời chiến dịch.'
  );

  return (
    <div className="min-h-screen bg-[#09090b] text-zinc-100">
      <header className="border-b border-zinc-800 bg-zinc-950/80 px-5 py-5 lg:px-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-black text-white">Promotion Center</h1>
            <p className="mt-1 text-xs text-zinc-500">{current.totalElements || 0} bản ghi</p>
          </div>
          <div className="flex items-center gap-2">
            <button type="button" onClick={() => void load()} className={`${buttonClass} border border-zinc-700 text-zinc-300 hover:bg-zinc-800`}>
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
            </button>
            <button type="button" onClick={() => setModal({ type: tab === 'campaigns' ? 'campaign' : 'promotion' })} className={`${buttonClass} bg-orange-500 text-white hover:bg-orange-600`}>
              <Plus className="h-4 w-4" /> {tab === 'campaigns' ? 'Chiến dịch' : 'Promotion'}
            </button>
          </div>
        </div>
      </header>

      <main className="px-5 py-5 lg:px-8">
        {message && <div role="alert" className={`mb-4 border-l-2 px-3 py-2 text-sm ${message.kind === 'error' ? 'border-red-500 bg-red-500/10 text-red-300' : 'border-emerald-500 bg-emerald-500/10 text-emerald-300'}`}>{message.text}</div>}

        <div className="mb-4 flex gap-1 border-b border-zinc-800">
          {[['campaigns', CalendarClock, 'Chiến dịch'], ['promotions', BadgePercent, 'Promotion']].map(([key, Icon, label]) => (
            <button key={key} type="button" onClick={() => { setTab(key); setStatus(''); setType(''); }}
              className={`flex items-center gap-2 border-b-2 px-4 py-3 text-xs font-bold ${tab === key ? 'border-orange-500 text-white' : 'border-transparent text-zinc-500 hover:text-zinc-200'}`}>
              <Icon className="h-4 w-4" /> {label}
            </button>
          ))}
        </div>

        <div className="mb-4 grid gap-3 md:grid-cols-[minmax(240px,1fr)_180px_180px]">
          <label className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-zinc-600" />
            <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm theo tên hoặc mã" className={`${fieldClass} pl-10`} />
          </label>
          <select value={status} onChange={event => setStatus(event.target.value)} className={fieldClass}>
            <option value="">Mọi trạng thái</option>
            {(tab === 'campaigns' ? ['DRAFT', 'SCHEDULED', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED'] : ['DRAFT', 'ACTIVE', 'PAUSED', 'DISABLED', 'EXPIRED']).map(value => <option key={value} value={value}>{labels[value]}</option>)}
          </select>
          {tab === 'promotions' && <select value={type} onChange={event => setType(event.target.value)} className={fieldClass}>
            <option value="">Mọi loại</option>
            {['AUTO', 'VOUCHER', 'COUPON'].map(value => <option key={value} value={value}>{labels[value]}</option>)}
          </select>}
        </div>

        <div className="overflow-x-auto border-y border-zinc-800">
          {loading ? <div className="flex h-48 items-center justify-center gap-2 text-sm text-zinc-500"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải</div>
            : tab === 'campaigns'
              ? <CampaignTable rows={campaigns.content} busy={busy} onEdit={item => setModal({ type: 'campaign', record: item })} onDelete={item => run(() => adminPromotionService.deleteCampaign(item.publicId), 'Đã xóa chiến dịch.')} onAction={campaignAction} />
              : <PromotionTable rows={promotions.content} busy={busy} onEdit={item => setModal({ type: 'promotion', record: item })} onIssue={item => setModal({ type: 'issue', record: item })}
                onClone={item => run(() => adminPromotionService.clonePromotion(item.publicId), 'Đã nhân bản promotion.')}
                onDelete={item => run(() => adminPromotionService.deletePromotion(item.publicId), 'Đã xóa promotion.')}
                onToggle={item => run(() => item.status === 'ACTIVE' ? adminPromotionService.pausePromotion(item.publicId) : adminPromotionService.activatePromotion(item.publicId), 'Đã cập nhật trạng thái promotion.')} />}
        </div>

        <div className="mt-4 flex items-center justify-between text-xs text-zinc-500">
          <span>Trang {current.totalPages ? page + 1 : 0} / {current.totalPages || 0}</span>
          <div className="flex gap-2">
            <IconButton title="Trang trước" disabled={page === 0} onClick={() => setPage(value => Math.max(0, value - 1))}><ChevronLeft className="h-4 w-4" /></IconButton>
            <IconButton title="Trang sau" disabled={current.last || page + 1 >= current.totalPages} onClick={() => setPage(value => value + 1)}><ChevronRight className="h-4 w-4" /></IconButton>
          </div>
        </div>
      </main>

      {modal?.type === 'campaign' && <CampaignModal record={modal.record} busy={busy} onClose={() => setModal(null)} onSave={(payload, editing) => run(
        () => editing ? adminPromotionService.updateCampaign(modal.record.publicId, payload) : adminPromotionService.createCampaign(payload),
        editing ? 'Đã cập nhật chiến dịch.' : 'Đã tạo chiến dịch.'
      )} />}
      {modal?.type === 'promotion' && <PromotionModal record={modal.record} campaigns={campaignOptions} busy={busy} onClose={() => setModal(null)} onSave={(payload, editing) => run(
        () => editing ? adminPromotionService.updatePromotion(modal.record.publicId, payload) : adminPromotionService.createPromotion(payload),
        editing ? 'Đã cập nhật promotion.' : 'Đã tạo promotion.'
      )} />}
      {modal?.type === 'issue' && <IssueModal promotion={modal.record} busy={busy} onClose={() => setModal(null)} onIssue={ids => run(
        () => adminPromotionService.issuePromotion(modal.record.publicId, ids), 'Đã phát hành vào ví khách hàng.'
      )} />}
    </div>
  );
}

function CampaignTable({ rows, busy, onEdit, onDelete, onAction }) {
  return <table className="w-full min-w-[980px] text-left text-sm"><thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500"><tr>
    {['Chiến dịch', 'Trạng thái', 'Hiệu lực', 'Ngân sách', 'Lượt dùng', 'Thao tác'].map(value => <th key={value} className="px-4 py-3">{value}</th>)}
  </tr></thead><tbody className="divide-y divide-zinc-800">{rows.map(row => <tr key={row.publicId} className="hover:bg-zinc-900/70">
    <td className="px-4 py-4"><p className="font-bold text-white">{row.name}</p><p className="mt-1 font-mono text-xs text-zinc-500">{row.code}</p></td>
    <td className="px-4 py-4"><div className="flex flex-wrap gap-1"><StatusBadge value={row.status} /><StatusBadge value={row.approvalStatus} /><StatusBadge value={row.legalStatus} /></div></td>
    <td className="px-4 py-4 text-xs text-zinc-400">{dateTime(row.startAt)}<br />{dateTime(row.endAt)}</td>
    <td className="px-4 py-4 text-xs"><p className="font-bold text-white">{money(row.budgetUsed)} / {money(row.budgetAmount)}</p><p className="mt-1 text-zinc-500">Đang giữ {money(row.budgetReserved)}</p></td>
    <td className="px-4 py-4 text-zinc-300">{row.redemptionCount || 0}{row.maxRedemptions ? ` / ${row.maxRedemptions}` : ''}</td>
    <td className="px-4 py-4"><div className="flex flex-wrap gap-1">
      <IconButton title="Sửa" onClick={() => onEdit(row)} disabled={busy || row.status !== 'DRAFT'}><Edit3 className="h-4 w-4" /></IconButton>
      {row.approvalStatus === 'DRAFT' && <IconButton title="Gửi duyệt" onClick={() => onAction(row, 'SUBMIT')} disabled={busy}><Send className="h-4 w-4" /></IconButton>}
      {row.approvalStatus === 'PENDING' && <IconButton title="Phê duyệt" onClick={() => onAction(row, 'APPROVE')} disabled={busy}><Check className="h-4 w-4" /></IconButton>}
      {row.legalStatus !== 'PASSED' && <IconButton title="Duyệt pháp lý" onClick={() => onAction(row, 'LEGAL')} disabled={busy}><Check className="h-4 w-4" /></IconButton>}
      {row.status === 'DRAFT' && row.approvalStatus === 'APPROVED' && row.legalStatus === 'PASSED' && <IconButton title="Xuất bản" onClick={() => onAction(row, 'PUBLISH')} disabled={busy}><CalendarClock className="h-4 w-4" /></IconButton>}
      {['SCHEDULED', 'PAUSED'].includes(row.status) && <IconButton title="Kích hoạt" onClick={() => onAction(row, 'ACTIVATE')} disabled={busy}><Play className="h-4 w-4" /></IconButton>}
      {row.status === 'ACTIVE' && <IconButton title="Tạm dừng" onClick={() => onAction(row, 'PAUSE')} disabled={busy}><CirclePause className="h-4 w-4" /></IconButton>}
      <IconButton title="Xóa" danger onClick={() => onDelete(row)} disabled={busy || row.status !== 'DRAFT'}><Trash2 className="h-4 w-4" /></IconButton>
    </div></td>
  </tr>)}</tbody></table>;
}

function PromotionTable({ rows, busy, onEdit, onIssue, onClone, onDelete, onToggle }) {
  return <table className="w-full min-w-[940px] text-left text-sm"><thead className="bg-zinc-950 text-[10px] font-bold uppercase text-zinc-500"><tr>
    {['Promotion', 'Phân phối', 'Trạng thái', 'Hiệu lực', 'Sử dụng', 'Thao tác'].map(value => <th key={value} className="px-4 py-3">{value}</th>)}
  </tr></thead><tbody className="divide-y divide-zinc-800">{rows.map(row => <tr key={row.publicId} className="hover:bg-zinc-900/70">
    <td className="px-4 py-4"><p className="font-bold text-white">{row.name}</p><p className="mt-1 font-mono text-xs text-zinc-500">{row.code || 'Không cần mã'}</p></td>
    <td className="px-4 py-4"><StatusBadge value={row.promotionType} />{row.publicVisible && <p className="mt-2 text-[10px] font-bold text-sky-300">Công khai</p>}</td>
    <td className="px-4 py-4"><StatusBadge value={row.status} /></td>
    <td className="px-4 py-4 text-xs text-zinc-400">{dateTime(row.validFrom)}<br />{dateTime(row.validTo)}</td>
    <td className="px-4 py-4 text-zinc-300">{row.redemptionCount || 0}{row.maxRedemptions ? ` / ${row.maxRedemptions}` : ''}</td>
    <td className="px-4 py-4"><div className="flex gap-1">
      <IconButton title="Sửa" onClick={() => onEdit(row)} disabled={busy || row.status !== 'DRAFT'}><Edit3 className="h-4 w-4" /></IconButton>
      <IconButton title={row.status === 'ACTIVE' ? 'Tạm dừng' : 'Kích hoạt'} onClick={() => onToggle(row)} disabled={busy || !['DRAFT', 'ACTIVE', 'PAUSED'].includes(row.status)}>{row.status === 'ACTIVE' ? <CirclePause className="h-4 w-4" /> : <Play className="h-4 w-4" />}</IconButton>
      <IconButton title="Nhân bản" onClick={() => onClone(row)} disabled={busy}><Copy className="h-4 w-4" /></IconButton>
      {row.promotionType !== 'AUTO' && <IconButton title="Phát hành" onClick={() => onIssue(row)} disabled={busy}><Users className="h-4 w-4" /></IconButton>}
      <IconButton title="Xóa" danger onClick={() => onDelete(row)} disabled={busy || row.status === 'ACTIVE'}><Trash2 className="h-4 w-4" /></IconButton>
    </div></td>
  </tr>)}</tbody></table>;
}

function ModalShell({ title, icon: Icon, onClose, children }) {
  return <div className="fixed inset-0 z-[90] flex items-center justify-center bg-black/80 p-4" onMouseDown={event => event.target === event.currentTarget && onClose()}>
    <section role="dialog" aria-modal="true" className="max-h-[92vh] w-full max-w-3xl overflow-y-auto rounded-lg border border-zinc-700 bg-zinc-900 shadow-2xl">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-zinc-800 bg-zinc-900 px-5 py-4"><div className="flex items-center gap-2"><Icon className="h-5 w-5 text-orange-400" /><h2 className="font-black text-white">{title}</h2></div><IconButton title="Đóng" onClick={onClose}><X className="h-4 w-4" /></IconButton></header>
      {children}
    </section>
  </div>;
}

function Field({ label, children, wide = false }) {
  return <label className={wide ? 'md:col-span-2' : ''}><span className="mb-1.5 block text-xs font-bold text-zinc-400">{label}</span>{children}</label>;
}

function Toggle({ checked, onChange, label }) {
  return <label className="flex cursor-pointer items-center gap-2 text-xs font-bold text-zinc-300"><input type="checkbox" checked={checked} onChange={event => onChange(event.target.checked)} className="h-4 w-4 accent-orange-500" />{label}</label>;
}

function CampaignModal({ record, busy, onClose, onSave }) {
  const editing = Boolean(record);
  const [form, setForm] = useState(() => ({
    code: record?.code || '', name: record?.name || '', description: record?.description || '',
    priority: record?.priority ?? 100, stackable: record?.stackable ?? false,
    exclusiveCampaign: record?.exclusiveCampaign ?? false, autoActivate: record?.autoActivate ?? true,
    autoComplete: record?.autoComplete ?? true, autoPauseWhenBudgetExceeded: record?.autoPauseWhenBudgetExceeded ?? true,
    timezone: record?.timezone || 'Asia/Ho_Chi_Minh', startAt: toLocalInput(record?.startAt),
    endAt: toLocalInput(record?.endAt || Date.now() + 30 * 86400_000), budgetAmount: record?.budgetAmount ?? 100000000,
    maxRedemptions: record?.maxRedemptions ?? '', maxRedemptionsPerUser: record?.maxRedemptionsPerUser ?? 1,
    legalNotificationRef: record?.legalNotificationRef || '', remarks: record?.remarks || '',
  }));
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const submit = event => {
    event.preventDefault();
    const payload = { ...form, priority: Number(form.priority), budgetAmount: Number(form.budgetAmount), maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null, maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser), startAt: fromLocalInput(form.startAt), endAt: fromLocalInput(form.endAt), legalNotificationRef: form.legalNotificationRef || null, remarks: form.remarks || null };
    if (editing) delete payload.code;
    onSave(payload, editing);
  };
  return <ModalShell title={editing ? 'Sửa chiến dịch' : 'Tạo chiến dịch'} icon={CalendarClock} onClose={onClose}><form onSubmit={submit} className="grid gap-4 p-5 md:grid-cols-2">
    <Field label="Mã chiến dịch"><input required disabled={editing} value={form.code} onChange={e => update('code', e.target.value.toUpperCase())} className={fieldClass} /></Field>
    <Field label="Tên chiến dịch"><input required value={form.name} onChange={e => update('name', e.target.value)} className={fieldClass} /></Field>
    <Field label="Mô tả" wide><textarea value={form.description} onChange={e => update('description', e.target.value)} className={`${fieldClass} h-20 py-2`} /></Field>
    <Field label="Bắt đầu"><input required type="datetime-local" value={form.startAt} onChange={e => update('startAt', e.target.value)} className={fieldClass} /></Field>
    <Field label="Kết thúc"><input required type="datetime-local" value={form.endAt} onChange={e => update('endAt', e.target.value)} className={fieldClass} /></Field>
    <Field label="Ngân sách"><input required min="1" type="number" value={form.budgetAmount} onChange={e => update('budgetAmount', e.target.value)} className={fieldClass} /></Field>
    <Field label="Ưu tiên"><input required min="0" type="number" value={form.priority} onChange={e => update('priority', e.target.value)} className={fieldClass} /></Field>
    <Field label="Tổng lượt tối đa"><input min="1" type="number" value={form.maxRedemptions} onChange={e => update('maxRedemptions', e.target.value)} className={fieldClass} /></Field>
    <Field label="Lượt tối đa mỗi khách"><input required min="1" type="number" value={form.maxRedemptionsPerUser} onChange={e => update('maxRedemptionsPerUser', e.target.value)} className={fieldClass} /></Field>
    <div className="grid gap-3 md:col-span-2 sm:grid-cols-2"><Toggle checked={form.autoActivate} onChange={v => update('autoActivate', v)} label="Tự kích hoạt" /><Toggle checked={form.autoComplete} onChange={v => update('autoComplete', v)} label="Tự hoàn tất" /><Toggle checked={form.autoPauseWhenBudgetExceeded} onChange={v => update('autoPauseWhenBudgetExceeded', v)} label="Dừng khi hết ngân sách" /><Toggle checked={form.exclusiveCampaign} onChange={v => update('exclusiveCampaign', v)} label="Chiến dịch độc quyền" /></div>
    <div className="flex justify-end gap-2 border-t border-zinc-800 pt-4 md:col-span-2"><button type="button" onClick={onClose} className={`${buttonClass} border border-zinc-700 text-zinc-300`}>Hủy</button><button disabled={busy} className={`${buttonClass} bg-orange-500 text-white`}>{busy && <Loader2 className="h-4 w-4 animate-spin" />} Lưu</button></div>
  </form></ModalShell>;
}

function PromotionModal({ record, campaigns, busy, onClose, onSave }) {
  const editing = Boolean(record);
  const existingAction = Array.isArray(record?.actionsJson) ? record.actionsJson[0] : record?.actionsJson || {};
  const [form, setForm] = useState(() => ({
    campaignPublicId: record?.campaignPublicId || campaigns[0]?.value || '', promotionType: record?.promotionType || 'AUTO',
    code: record?.code || '', name: record?.name || '', description: record?.description || '', publicVisible: record?.publicVisible || false,
    priority: record?.priority ?? 100, stackable: record?.stackable ?? false, maxRedemptions: record?.maxRedemptions ?? '',
    maxRedemptionsPerUser: record?.maxRedemptionsPerUser ?? 1, validFrom: toLocalInput(record?.validFrom || campaigns[0]?.item?.startAt),
    validTo: toLocalInput(record?.validTo || campaigns[0]?.item?.endAt || Date.now() + 30 * 86400_000),
    conditionsText: JSON.stringify(record?.conditionsJson || {}, null, 2),
    actionType: existingAction.type || existingAction.discountType || 'PERCENTAGE',
    actionValue: existingAction.value ?? existingAction.discountValue ?? existingAction.amount ?? existingAction.percentage ?? 10,
    maxDiscountAmount: existingAction.maxDiscountAmount ?? '',
  }));
  const [jsonError, setJsonError] = useState('');
  const update = (key, value) => setForm(current => ({ ...current, [key]: value }));
  const submit = event => {
    event.preventDefault();
    let conditionsJson;
    try { conditionsJson = JSON.parse(form.conditionsText || '{}'); } catch { setJsonError('Điều kiện phải là JSON hợp lệ.'); return; }
    const actionsJson = form.actionType === 'FULL_DISCOUNT'
      ? { type: 'FULL_DISCOUNT' }
      : { type: form.actionType, value: Number(form.actionValue), ...(form.actionType === 'PERCENTAGE' && form.maxDiscountAmount ? { maxDiscountAmount: Number(form.maxDiscountAmount) } : {}) };
    onSave({
      campaignPublicId: form.campaignPublicId, promotionType: form.promotionType,
      code: form.promotionType === 'AUTO' ? null : form.code.trim().toUpperCase(), name: form.name, description: form.description,
      publicVisible: form.promotionType === 'VOUCHER' && form.publicVisible, priority: Number(form.priority), stackable: form.stackable,
      conditionsJson, actionsJson, metadataJson: {}, maxRedemptions: form.maxRedemptions ? Number(form.maxRedemptions) : null,
      maxRedemptionsPerUser: Number(form.maxRedemptionsPerUser), validFrom: fromLocalInput(form.validFrom), validTo: fromLocalInput(form.validTo),
    }, editing);
  };
  return <ModalShell title={editing ? 'Sửa promotion' : 'Tạo promotion'} icon={BadgePercent} onClose={onClose}><form onSubmit={submit} className="grid gap-4 p-5 md:grid-cols-2">
    <Field label="Chiến dịch" wide><select required value={form.campaignPublicId} onChange={e => update('campaignPublicId', e.target.value)} className={fieldClass}><option value="">Chọn chiến dịch</option>{campaigns.map(item => <option key={item.value} value={item.value}>{item.label}</option>)}</select></Field>
    <div className="grid grid-cols-3 gap-1 md:col-span-2">{['AUTO', 'VOUCHER', 'COUPON'].map(value => <button type="button" key={value} disabled={editing} onClick={() => update('promotionType', value)} className={`${buttonClass} ${form.promotionType === value ? 'bg-orange-500 text-white' : 'border border-zinc-700 text-zinc-400'}`}>{labels[value]}</button>)}</div>
    <Field label="Tên promotion"><input required value={form.name} onChange={e => update('name', e.target.value)} className={fieldClass} /></Field>
    <Field label="Mã">{form.promotionType === 'AUTO' ? <div className="flex h-10 items-center text-xs font-bold text-zinc-500">Tự áp dụng, không cần mã</div> : <input required value={form.code} onChange={e => update('code', e.target.value.toUpperCase())} className={fieldClass} />}</Field>
    <Field label="Kiểu giảm"><select value={form.actionType} onChange={e => update('actionType', e.target.value)} className={fieldClass}><option value="PERCENTAGE">Giảm phần trăm</option><option value="FIXED_AMOUNT">Giảm số tiền</option><option value="FULL_DISCOUNT">Miễn phí toàn bộ</option></select></Field>
    <Field label={form.actionType === 'PERCENTAGE' ? 'Phần trăm (tối đa 50)' : 'Giá trị giảm'}>{form.actionType === 'FULL_DISCOUNT' ? <div className="flex h-10 items-center gap-2 text-xs font-bold text-emerald-300"><Gift className="h-4 w-4" /> Giảm 100% cho mọi khách đủ điều kiện</div> : <input required min="1" max={form.actionType === 'PERCENTAGE' ? 50 : undefined} type="number" value={form.actionValue} onChange={e => update('actionValue', e.target.value)} className={fieldClass} />}</Field>
    {form.actionType === 'PERCENTAGE' && <Field label="Mức giảm tối đa"><input min="1" type="number" value={form.maxDiscountAmount} onChange={e => update('maxDiscountAmount', e.target.value)} className={fieldClass} /></Field>}
    <Field label="Ưu tiên"><input min="0" type="number" value={form.priority} onChange={e => update('priority', e.target.value)} className={fieldClass} /></Field>
    <Field label="Bắt đầu"><input required type="datetime-local" value={form.validFrom} onChange={e => update('validFrom', e.target.value)} className={fieldClass} /></Field>
    <Field label="Kết thúc"><input required type="datetime-local" value={form.validTo} onChange={e => update('validTo', e.target.value)} className={fieldClass} /></Field>
    <Field label="Tổng lượt tối đa"><input min="1" type="number" value={form.maxRedemptions} onChange={e => update('maxRedemptions', e.target.value)} className={fieldClass} /></Field>
    <Field label="Lượt tối đa mỗi khách"><input required min="1" type="number" value={form.maxRedemptionsPerUser} onChange={e => update('maxRedemptionsPerUser', e.target.value)} className={fieldClass} /></Field>
    <Field label="Điều kiện JSON" wide><textarea value={form.conditionsText} onChange={e => { update('conditionsText', e.target.value); setJsonError(''); }} className={`${fieldClass} h-28 py-2 font-mono text-xs`} />{jsonError && <span className="mt-1 block text-xs text-red-400">{jsonError}</span>}</Field>
    <div className="flex flex-wrap gap-4 md:col-span-2"><Toggle checked={form.stackable} onChange={v => update('stackable', v)} label="Cho cộng dồn" />{form.promotionType === 'VOUCHER' && <Toggle checked={form.publicVisible} onChange={v => update('publicVisible', v)} label="Công khai để khách tự nhận" />}</div>
    <div className="flex justify-end gap-2 border-t border-zinc-800 pt-4 md:col-span-2"><button type="button" onClick={onClose} className={`${buttonClass} border border-zinc-700 text-zinc-300`}>Hủy</button><button disabled={busy || campaigns.length === 0} className={`${buttonClass} bg-orange-500 text-white`}>{busy && <Loader2 className="h-4 w-4 animate-spin" />} Lưu</button></div>
  </form></ModalShell>;
}

function IssueModal({ promotion, busy, onClose, onIssue }) {
  const [value, setValue] = useState('');
  const ids = value.split(/[,\n]/).map(item => item.trim()).filter(Boolean);
  return <ModalShell title={`Phát hành ${promotion.name}`} icon={Users} onClose={onClose}><div className="p-5"><textarea value={value} onChange={event => setValue(event.target.value)} placeholder="Mỗi dòng một Account ID hoặc UUID" className={`${fieldClass} h-40 py-3 font-mono`} /><div className="mt-4 flex items-center justify-between"><span className="text-xs text-zinc-500">{new Set(ids).size} khách hàng</span><div className="flex gap-2"><button type="button" onClick={onClose} className={`${buttonClass} border border-zinc-700 text-zinc-300`}>Hủy</button><button type="button" disabled={busy || ids.length === 0} onClick={() => onIssue([...new Set(ids)])} className={`${buttonClass} bg-orange-500 text-white`}>{busy ? <Loader2 className="h-4 w-4 animate-spin" /> : <Send className="h-4 w-4" />} Phát hành</button></div></div></div></ModalShell>;
}
