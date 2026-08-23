import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  AlertTriangle,
  BadgePercent,
  Bot,
  CalendarClock,
  CheckCircle2,
  ChevronRight,
  CircleGauge,
  Gift,
  Loader2,
  RefreshCw,
  Search,
  Send,
  ShieldCheck,
  TicketCheck,
  X,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import managerPromotionService from '../services/managerPromotionService';

const idleState = () => ({ status: 'idle', data: null, message: '' });
const idleViews = () => Object.fromEntries(
  viewDefinitions.map(item => [item.key, idleState()]),
);

const viewDefinitions = [
  { key: 'overview', label: 'Tổng quan tại rạp', icon: CircleGauge },
  { key: 'campaigns', label: 'Chương trình tại rạp', icon: BadgePercent },
  { key: 'automations', label: 'Luồng tự động', icon: Bot },
  { key: 'distribution', label: 'Quyền lợi được phép', icon: Gift },
  { key: 'incidents', label: 'Sự cố tại rạp', icon: AlertTriangle, capability: 'canViewLocalIncidents' },
];

const statusLabels = {
  DRAFT: 'Bản nháp địa phương',
  PENDING: 'Đang chờ duyệt',
  SCHEDULED: 'Sắp chạy',
  ACTIVE: 'Đang chạy',
  PAUSED: 'Tạm dừng',
  COMPLETED: 'Đã kết thúc',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã trả lại',
  PENDING_APPROVAL: 'Chờ phê duyệt',
  AUDIENCE_READY: 'Đã xác định đối tượng',
  ISSUING: 'Đang cấp',
  COMPLETED_NO_AUDIENCE: 'Hoàn tất, không có đối tượng',
  PARTIAL_SUCCESS: 'Hoàn tất một phần',
  FAILED: 'Thất bại',
  REVIEW_REQUIRED: 'Cần kiểm tra',
  OPEN: 'Cần xử lý',
  IN_REVIEW: 'Đang xử lý',
};

const stateMessage = (error) => {
  const status = error?.response?.status || error?.status;
  if (status === 403) {
    return {
      status: 'forbidden',
      message: 'Bạn chưa được cấp quyền xem nội dung này. Liên hệ quản trị viên hệ thống để được hỗ trợ.',
    };
  }
  return {
    status: 'error',
    message: 'Không thể tải dữ liệu khuyến mãi của rạp. Vui lòng thử lại.',
  };
};

const dateTime = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
  : 'Chưa xác định';

const quotaText = item => item?.quota == null
  ? 'Không giới hạn lượt'
  : `Còn ${Number(item.remaining || 0).toLocaleString('vi-VN')} / ${Number(item.quota).toLocaleString('vi-VN')} lượt`;

function StatusPill({ value }) {
  if (!value) return <span className="text-xs text-zinc-600">Chưa có lần chạy</span>;
  const warning = ['FAILED', 'REVIEW_REQUIRED', 'REJECTED'].includes(value);
  const active = ['ACTIVE', 'COMPLETED', 'APPROVED'].includes(value);
  return (
    <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${
      warning
        ? 'border-red-500/25 bg-red-500/10 text-red-300'
        : active
          ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300'
          : 'border-amber-500/25 bg-amber-500/10 text-amber-200'
    }`}>
      {statusLabels[value] || value}
    </span>
  );
}

function RequestState({ state, onRetry, emptyTitle, emptyDescription }) {
  if (state.status === 'loading' || state.status === 'idle') {
    return (
      <div className="flex min-h-64 items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/[0.02] text-sm text-zinc-500">
        <Loader2 className="h-4 w-4 animate-spin" /> Đang tải dữ liệu của rạp
      </div>
    );
  }
  if (state.status === 'forbidden' || state.status === 'error') {
    return (
      <div role="alert" className="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-red-500/20 bg-red-500/[0.04] px-6 text-center">
        <AlertTriangle className="h-8 w-8 text-red-300" />
        <h2 className="mt-4 text-base font-black text-white">
          {state.status === 'forbidden' ? 'Chưa được cấp quyền' : 'Tải dữ liệu thất bại'}
        </h2>
        <p className="mt-2 max-w-xl text-sm leading-6 text-zinc-400">{state.message}</p>
        <button type="button" onClick={onRetry} className="mt-5 inline-flex items-center gap-2 rounded-xl border border-white/15 px-4 py-2 text-xs font-black text-white hover:bg-white/5">
          <RefreshCw className="h-4 w-4" /> Thử lại
        </button>
      </div>
    );
  }
  if (state.status === 'empty') {
    return (
      <div className="flex min-h-64 flex-col items-center justify-center rounded-2xl border border-white/10 bg-white/[0.02] px-6 text-center">
        <CheckCircle2 className="h-8 w-8 text-emerald-400" />
        <h2 className="mt-4 text-base font-black text-white">{emptyTitle}</h2>
        <p className="mt-2 text-sm text-zinc-500">{emptyDescription}</p>
      </div>
    );
  }
  return null;
}

function SummaryCard({ icon: Icon, label, value, tone = 'orange' }) {
  const tones = {
    orange: 'border-orange-500/20 bg-orange-500/[0.05] text-orange-300',
    blue: 'border-sky-500/20 bg-sky-500/[0.05] text-sky-300',
    amber: 'border-amber-500/20 bg-amber-500/[0.05] text-amber-300',
    red: 'border-red-500/20 bg-red-500/[0.05] text-red-300',
  };
  return (
    <div className={`rounded-2xl border p-4 ${tones[tone]}`}>
      <Icon className="h-5 w-5" />
      <p className="mt-5 text-2xl font-black text-white">{value}</p>
      <p className="mt-1 text-xs font-bold text-zinc-500">{label}</p>
    </div>
  );
}

function Overview({ data, onNavigate }) {
  const tasks = data.tasks || [];
  return (
    <div className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard icon={TicketCheck} label="Chương trình đang chạy" value={data.activeCampaignCount || 0} />
        <SummaryCard icon={CalendarClock} label="Chương trình sắp bắt đầu" value={data.upcomingCampaignCount || 0} tone="blue" />
        <SummaryCard icon={Gift} label="Quyền lợi gần hết hạn mức" value={data.lowQuotaBenefitCount || 0} tone="amber" />
        <SummaryCard icon={AlertTriangle} label="Sự cố tại rạp cần xử lý" value={data.openIncidentCount || 0} tone="red" />
      </div>

      <section className="rounded-2xl border border-white/10 bg-[#0d0d10] p-5">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-[10px] font-black uppercase tracking-[0.18em] text-brand-orange">Ưu tiên vận hành</p>
            <h2 className="mt-1 text-lg font-black text-white">Việc cần chú ý tại rạp</h2>
          </div>
          <ShieldCheck className="h-6 w-6 text-emerald-400" />
        </div>
        {tasks.length === 0 ? (
          <div className="mt-5 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.04] p-5 text-center">
            <CheckCircle2 className="mx-auto h-7 w-7 text-emerald-400" />
            <p className="mt-3 text-sm font-black text-white">Không có việc khẩn cấp cần xử lý</p>
            <p className="mt-1 text-xs text-zinc-500">Dữ liệu của rạp đã tải thành công và hiện không có cảnh báo mới.</p>
          </div>
        ) : (
          <div className="mt-5 divide-y divide-white/10">
            {tasks.map(task => (
              <button key={task.key} type="button" onClick={() => onNavigate(task.targetView)} className="flex w-full items-center gap-4 py-4 text-left hover:bg-white/[0.02]">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-orange-500/10 text-orange-300"><AlertTriangle className="h-4 w-4" /></span>
                <span className="min-w-0 flex-1"><span className="block text-sm font-black text-white">{task.title}</span><span className="mt-1 block text-xs text-zinc-500">{task.description}</span></span>
                <ChevronRight className="h-4 w-4 text-zinc-600" />
              </button>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function Campaigns({ items, query }) {
  const filtered = items.filter(item => `${item.name} ${item.code}`.toLowerCase().includes(query.toLowerCase()));
  if (filtered.length === 0) return null;
  return (
    <div className="grid gap-4 xl:grid-cols-2">
      {filtered.map(item => (
        <article key={item.publicId} className="rounded-2xl border border-white/10 bg-[#0d0d10] p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <span className={`inline-flex rounded-full px-2.5 py-1 text-[10px] font-black uppercase ${item.source === 'CENTRAL' ? 'bg-sky-500/10 text-sky-300' : 'bg-orange-500/10 text-orange-300'}`}>
                {item.source === 'CENTRAL' ? 'Chương trình toàn chuỗi' : 'Dành cho rạp này'}
              </span>
              <h3 className="mt-3 text-base font-black text-white">{item.name}</h3>
              <p className="mt-1 text-xs text-zinc-600">{item.code}</p>
            </div>
            <StatusPill value={item.status} />
          </div>
          <p className="mt-4 text-sm leading-6 text-zinc-400">{item.description || 'Chưa có mô tả vận hành.'}</p>
          <div className="mt-5 grid gap-3 border-t border-white/10 pt-4 text-xs sm:grid-cols-2">
            <div><p className="text-zinc-600">Hiệu lực</p><p className="mt-1 font-bold text-zinc-300">{dateTime(item.startAt)} — {dateTime(item.endAt)}</p></div>
            <div><p className="text-zinc-600">Hạn mức</p><p className="mt-1 font-bold text-zinc-300">{quotaText(item)}</p></div>
          </div>
          <div className="mt-4 rounded-xl bg-white/[0.025] p-3">
            <p className="text-[10px] font-black uppercase tracking-wide text-zinc-600">Nhân viên cần biết</p>
            {item.benefits?.length ? item.benefits.map(benefit => (
              <div key={benefit.publicId} className="mt-2 flex items-center justify-between gap-3 text-xs"><span className="text-zinc-300">{benefit.name}</span><StatusPill value={benefit.status} /></div>
            )) : <p className="mt-2 text-xs text-zinc-500">Chưa có quyền lợi áp dụng tại rạp.</p>}
          </div>
        </article>
      ))}
    </div>
  );
}

function Automations({ items }) {
  return (
    <div className="space-y-3">
      {items.map(item => (
        <article key={item.publicId} className="grid gap-4 rounded-2xl border border-white/10 bg-[#0d0d10] p-5 md:grid-cols-[1fr_auto] md:items-center">
          <div className="flex items-start gap-4">
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-sky-500/10 text-sky-300"><Bot className="h-5 w-5" /></span>
            <div><div className="flex flex-wrap items-center gap-2"><h3 className="text-sm font-black text-white">{item.name}</h3><span className="rounded-full border border-white/10 px-2 py-0.5 text-[9px] font-black uppercase text-zinc-500">Chỉ đọc</span></div><p className="mt-1 text-xs leading-5 text-zinc-500">{item.description || 'Luồng trung tâm đang áp dụng cho rạp này.'}</p></div>
          </div>
          <div className="md:text-right"><StatusPill value={item.status} /><p className="mt-2 text-xs text-zinc-600">Lần gần nhất: {dateTime(item.latestRunAt)}</p></div>
        </article>
      ))}
    </div>
  );
}

function Distribution({ items, onIssue }) {
  return (
    <div className="space-y-3">
      {items.map(item => (
        <article key={item.publicId} className="rounded-2xl border border-white/10 bg-[#0d0d10] p-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="flex min-w-0 items-start gap-4">
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-orange-500/10 text-orange-300"><Gift className="h-5 w-5" /></span>
              <div><p className="text-xs font-bold text-zinc-600">{item.campaignName}</p><h3 className="mt-1 text-sm font-black text-white">{item.name}</h3><p className="mt-2 text-xs leading-5 text-zinc-500">{item.staffGuidance}</p></div>
            </div>
            <div className="text-right"><StatusPill value={item.status} /><p className="mt-2 text-xs font-bold text-zinc-400">{quotaText(item)}</p></div>
          </div>
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-white/10 pt-4">
            <p className="text-xs text-zinc-600">Hiệu lực đến {dateTime(item.validTo)}</p>
            {item.canDistribute ? (
              <button type="button" onClick={() => onIssue(item)} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 text-xs font-black text-black hover:bg-orange-400"><Send className="h-4 w-4" /> Cấp quyền lợi</button>
            ) : <span className="text-xs font-bold text-zinc-600">Chỉ xem</span>}
          </div>
        </article>
      ))}
    </div>
  );
}

function Incidents({ items }) {
  return (
    <div className="space-y-3">
      {items.map(item => (
        <article key={item.publicId} className="flex items-start gap-4 rounded-2xl border border-red-500/15 bg-red-500/[0.035] p-5">
          <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" />
          <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center justify-between gap-3"><h3 className="text-sm font-black text-white">{item.businessName}</h3><StatusPill value={item.status} /></div><p className="mt-2 text-xs leading-5 text-zinc-400">{item.summary}</p><p className="mt-3 text-[11px] text-zinc-600">Phát sinh lúc {dateTime(item.createdAt)}</p></div>
        </article>
      ))}
    </div>
  );
}

function IssueDialog({ item, busy, error, onClose, onSubmit }) {
  const [value, setValue] = useState('');
  const userPublicIds = value.split(/[\s,;]+/).map(entry => entry.trim()).filter(Boolean);
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4">
      <form onSubmit={(event) => { event.preventDefault(); onSubmit(userPublicIds); }} className="w-full max-w-lg rounded-2xl border border-white/10 bg-[#111115] p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4"><div><p className="text-[10px] font-black uppercase tracking-wider text-brand-orange">Phân phối tại rạp</p><h2 className="mt-1 text-lg font-black text-white">{item.name}</h2></div><button type="button" onClick={onClose} className="rounded-lg p-2 text-zinc-500 hover:bg-white/5 hover:text-white"><X className="h-4 w-4" /></button></div>
        <p className="mt-4 rounded-xl border border-emerald-500/15 bg-emerald-500/[0.04] p-3 text-xs leading-5 text-emerald-200">Hành động chỉ áp dụng tại rạp đang chọn và được kiểm tra lại bằng hạn mức phía máy chủ.</p>
        <label className="mt-5 block text-xs font-black text-zinc-300">Mã khách hàng
          <textarea value={value} onChange={event => setValue(event.target.value)} rows={4} placeholder="Nhập một hoặc nhiều mã, cách nhau bằng dấu phẩy" className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm font-medium text-white outline-none focus:border-brand-orange/50" />
        </label>
        {error && <p role="alert" className="mt-3 text-xs text-red-300">{error}</p>}
        <div className="mt-5 flex justify-end gap-2"><button type="button" onClick={onClose} className="rounded-xl border border-white/10 px-4 py-2 text-xs font-black text-zinc-300">Hủy</button><button type="submit" disabled={busy || userPublicIds.length === 0} className="inline-flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2 text-xs font-black text-black disabled:opacity-40">{busy && <Loader2 className="h-4 w-4 animate-spin" />} Xác nhận cấp</button></div>
      </form>
    </div>
  );
}

export default function ManagerPromotionCenterPage() {
  const { user } = useAuth();
  const { selectedCinema, selectedCinemaId, cinemaState } = useOutletContext();
  const [view, setView] = useState('overview');
  const [query, setQuery] = useState('');
  const [scopeCache, setScopeCache] = useState(() => ({
    cinemaPublicId: selectedCinemaId,
    states: idleViews(),
  }));
  const states = scopeCache.cinemaPublicId === selectedCinemaId
    ? scopeCache.states
    : idleViews();
  const setStates = useCallback((updater) => {
    setScopeCache(previous => {
      const base = previous.cinemaPublicId === selectedCinemaId
        ? previous.states
        : idleViews();
      return {
        cinemaPublicId: selectedCinemaId,
        states: typeof updater === 'function' ? updater(base) : updater,
      };
    });
  }, [selectedCinemaId]);
  const [capabilities, setCapabilities] = useState({
    canViewCinemaPromotions: true,
    canViewLocalIncidents: user?.permissions?.includes('PROMOTION_AUDIT_VIEW'),
    canDistributeLocalBenefit: user?.permissions?.includes('PROMOTION_DISTRIBUTE_LOCAL'),
  });
  const [issueItem, setIssueItem] = useState(null);
  const [issueState, setIssueState] = useState({ busy: false, error: '' });
  const [notice, setNotice] = useState({ cinemaPublicId: '', text: '' });

  const visibleViews = useMemo(() => viewDefinitions.filter(
    item => !item.capability || capabilities?.[item.capability],
  ), [capabilities]);

  const loadView = useCallback(async (targetView, force = false) => {
    if (!selectedCinemaId) return;
    const current = states[targetView];
    if (!force && current?.status !== 'idle') return;
    setStates(previous => ({ ...previous, [targetView]: { status: 'loading', data: null, message: '' } }));
    try {
      let data;
      if (targetView === 'overview') data = await managerPromotionService.getWorkspace(selectedCinemaId);
      else if (targetView === 'campaigns') data = await managerPromotionService.getCampaigns(selectedCinemaId);
      else if (targetView === 'automations') data = await managerPromotionService.getAutomations(selectedCinemaId);
      else if (targetView === 'distribution') data = await managerPromotionService.getDistributionOptions(selectedCinemaId);
      else data = await managerPromotionService.getIncidents(selectedCinemaId);
      if (targetView === 'distribution' && Array.isArray(data)) {
        data = data.filter(item => item.distributionMode !== 'AUTOMATION_ONLY');
      }
      if (targetView === 'overview' && data?.capabilities) setCapabilities(data.capabilities);
      const empty = Array.isArray(data) && data.length === 0;
      setStates(previous => ({ ...previous, [targetView]: { status: empty ? 'empty' : 'success', data, message: '' } }));
    } catch (error) {
      const failure = stateMessage(error);
      setStates(previous => ({ ...previous, [targetView]: { ...failure, data: null } }));
    }
  }, [selectedCinemaId, setStates, states]);

  useEffect(() => {
    const timer = window.setTimeout(() => void loadView(view), 0);
    return () => window.clearTimeout(timer);
  }, [loadView, view]);

  const changeView = next => {
    setView(next);
    setQuery('');
  };

  const refresh = () => {
    setNotice({ cinemaPublicId: selectedCinemaId, text: '' });
    void loadView(view, true);
  };

  const submitIssue = async userPublicIds => {
    setIssueState({ busy: true, error: '' });
    try {
      const result = await managerPromotionService.issueBenefit(
        selectedCinemaId, issueItem.publicId, userPublicIds,
      );
      setIssueItem(null);
      setIssueState({ busy: false, error: '' });
      setNotice({
        cinemaPublicId: selectedCinemaId,
        text: `Đã cấp ${result?.issuedCount || 0} quyền lợi tại ${selectedCinema?.name || 'rạp'}.`,
      });
      await loadView('distribution', true);
      await loadView('overview', true);
    } catch (error) {
      const failure = stateMessage(error);
      setIssueState({ busy: false, error: failure.message });
    }
  };

  if (cinemaState?.loading || !selectedCinemaId) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center rounded-2xl border border-white/10 bg-white/[0.02] text-sm text-zinc-500">
        {cinemaState?.loading ? <><Loader2 className="mr-2 h-4 w-4 animate-spin" /> Đang xác định rạp được phân công</> : 'Bạn chưa có rạp được phân công.'}
      </div>
    );
  }

  const state = states[view] || idleState();
  const queryFilteredEmpty = view === 'campaigns' && state.status === 'success'
    && !(state.data || []).some(item => `${item.name} ${item.code}`.toLowerCase().includes(query.toLowerCase()));
  const emptyCopy = {
    campaigns: ['Chưa có chương trình áp dụng tại rạp', 'Chỉ chương trình toàn chuỗi hoặc chương trình dành cho rạp này mới xuất hiện.'],
    automations: ['Chưa có luồng tự động ảnh hưởng đến rạp', 'Luồng trung tâm sẽ xuất hiện ở chế độ chỉ đọc khi được áp dụng.'],
    distribution: ['Chưa có quyền lợi nào được phép phân phối', 'Quyền lợi AUTOMATION_ONLY và quyền lợi ngoài rạp luôn được ẩn.'],
    incidents: ['Không có sự cố tại rạp cần xử lý', 'Dữ liệu đã tải thành công và hiện không có cảnh báo cục bộ.'],
  };

  return (
    <div className="min-h-[calc(100vh-9rem)] text-zinc-100">
      <header className="border-b border-white/10 pb-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div><p className="text-[10px] font-black uppercase tracking-[0.2em] text-brand-orange">Khuyến mãi tại rạp</p><h1 className="mt-1 text-2xl font-black text-white">Trung tâm khuyến mãi</h1><p className="mt-2 text-sm text-zinc-500">Chỉ hiển thị chương trình và quyền lợi áp dụng cho {selectedCinema?.name || 'rạp được phân công'}.</p></div>
          <button type="button" onClick={refresh} className="inline-flex items-center gap-2 rounded-xl border border-white/15 px-4 py-2.5 text-xs font-black text-zinc-300 hover:bg-white/5"><RefreshCw className={`h-4 w-4 ${state.status === 'loading' ? 'animate-spin' : ''}`} /> Làm mới</button>
        </div>
        <nav aria-label="Khu vực khuyến mãi tại rạp" className="mt-6 flex gap-1 overflow-x-auto">
          {visibleViews.map(item => { const Icon = item.icon; return <button key={item.key} type="button" onClick={() => changeView(item.key)} className={`inline-flex shrink-0 items-center gap-2 rounded-xl px-4 py-3 text-xs font-black ${view === item.key ? 'bg-orange-500/10 text-orange-300' : 'text-zinc-500 hover:bg-white/[0.03] hover:text-white'}`}><Icon className="h-4 w-4" /> {item.label}</button>; })}
        </nav>
      </header>

      <main className="py-6">
        {notice.cinemaPublicId === selectedCinemaId && notice.text && <div role="status" className="mb-5 flex items-center gap-2 rounded-xl border border-emerald-500/20 bg-emerald-500/[0.05] px-4 py-3 text-sm text-emerald-300"><CheckCircle2 className="h-4 w-4" /> {notice.text}</div>}
        {view === 'campaigns' && state.status === 'success' && (
          <label className="relative mb-5 block"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-600" /><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Tìm chương trình tại rạp" className="min-h-11 w-full rounded-xl border border-white/10 bg-black/20 py-2 pl-10 pr-4 text-sm text-white outline-none focus:border-brand-orange/40" /></label>
        )}

        {state.status === 'success' ? (
          view === 'overview' ? <Overview data={state.data} onNavigate={changeView} />
            : view === 'campaigns' ? (queryFilteredEmpty ? <RequestState state={{ status: 'empty' }} onRetry={refresh} emptyTitle="Không tìm thấy chương trình phù hợp" emptyDescription="Thử một từ khóa khác." /> : <Campaigns items={state.data || []} query={query} />)
              : view === 'automations' ? <Automations items={state.data || []} />
                : view === 'distribution' ? <Distribution items={state.data || []} onIssue={setIssueItem} />
                  : <Incidents items={state.data || []} />
        ) : (
          <RequestState state={state} onRetry={refresh} emptyTitle={emptyCopy[view]?.[0] || 'Không có dữ liệu'} emptyDescription={emptyCopy[view]?.[1] || ''} />
        )}
      </main>

      {issueItem && <IssueDialog item={issueItem} busy={issueState.busy} error={issueState.error} onClose={() => { setIssueItem(null); setIssueState({ busy: false, error: '' }); }} onSubmit={submitIssue} />}
    </div>
  );
}
