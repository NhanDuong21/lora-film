import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { Download, Eye, RefreshCw, RotateCcw, Search, ShieldAlert } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import {
  exportAdminPayments,
  assignReconciliation,
  getPaymentOperations,
  paymentErrorMessage,
  replayPaymentOperation,
  resolveReconciliation,
  searchAdminPayments,
} from '../../services/paymentService';

const tabs = [
  { key: 'transactions', label: 'Giao dịch' },
  { key: 'webhooks', label: 'Webhook' },
  { key: 'outbox', label: 'Outbox' },
  { key: 'reconciliations', label: 'Đối soát' },
];

const viStatus = {
  PENDING: 'Chờ xử lý',
  PROCESSING: 'Đang xử lý',
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  CANCELLED: 'Đã hủy',
  EXPIRED: 'Hết hạn',
  PUBLISHED: 'Đã gửi',
  DEAD_LETTER: 'Cần xử lý',
  PROCESSED: 'Đã xử lý',
  REQUIRED: 'Cần đối soát',
  IN_REVIEW: 'Đang kiểm tra',
  RESOLVED: 'Đã giải quyết',
  NONE: 'Chưa cần đối soát',
  OPEN: 'Mới mở',
  IGNORED: 'Đã bỏ qua',
};

const money = (value, currency = 'VND') =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value || 0));

const badge = status => {
  if (['SUCCESS', 'PUBLISHED', 'PROCESSED', 'RESOLVED'].includes(status)) return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
  if (['FAILED', 'CANCELLED', 'EXPIRED', 'DEAD_LETTER'].includes(status)) return 'bg-red-500/10 text-red-400 border-red-500/30';
  return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
};

export default function AdminPaymentsPage() {
  const navigate = useNavigate();
  const { userRole, accountId } = useAuth();
  const { triggerToast, triggerConfirm, triggerAlert } = useOutletContext() || {};
  const isAdmin = (userRole || '').replace(/^ROLE_/, '') === 'ADMIN';
  const [activeTab, setActiveTab] = useState('transactions');
  const [items, setItems] = useState([]);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({ query: '', status: '', provider: '', reconciliationStatus: '' });
  const [reconciliationAction, setReconciliationAction] = useState(null);
  const [resolutionForm, setResolutionForm] = useState({
    resolutionCode: '',
    note: '',
    ignored: false,
  });

  const params = useMemo(() => ({
    page: 0,
    size: 50,
    ...(filters.query && { query: filters.query }),
    ...(filters.status && { status: filters.status }),
    ...(filters.provider && { provider: filters.provider }),
    ...(filters.reconciliationStatus && { reconciliationStatus: filters.reconciliationStatus }),
  }), [filters]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = activeTab === 'transactions'
        ? await searchAdminPayments(params)
        : await getPaymentOperations(activeTab, { page: 0, size: 50 });
      setItems(result?.content || []);
      setPageInfo({
        number: result?.number || 0,
        totalPages: result?.totalPages || 0,
        totalElements: result?.totalElements || 0,
      });
    } catch (error) {
      (triggerAlert || (() => {}))(paymentErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [activeTab, params, triggerAlert]);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const replay = async (kind, id) => {
    if (!isAdmin) return;
    const accepted = triggerConfirm
      ? await triggerConfirm('Đưa bản ghi này vào hàng đợi xử lý lại? Hệ thống vẫn giữ nguyên mã idempotency.')
      : true;
    if (!accepted) return;
    try {
      await replayPaymentOperation(kind, id);
      triggerToast?.('Đã đưa tác vụ vào xử lý lại.');
      load();
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    }
  };

  const submitReconciliation = async event => {
    event.preventDefault();
    if (!reconciliationAction || !isAdmin) return;
    setLoading(true);
    try {
      if (reconciliationAction.mode === 'assign') {
        await assignReconciliation(reconciliationAction.item.publicId, Number(accountId));
        triggerToast?.('Đã nhận xử lý hồ sơ đối soát.');
      } else {
        await resolveReconciliation(reconciliationAction.item.publicId, resolutionForm);
        triggerToast?.('Đã đóng hồ sơ đối soát.');
      }
      setReconciliationAction(null);
      setResolutionForm({ resolutionCode: '', note: '', ignored: false });
      await load();
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6 text-white">
      <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-6 lg:flex-row lg:items-end">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.22em] text-brand-orange">Vận hành thanh toán</p>
          <h1 className="mt-2 text-3xl font-black uppercase">Giao dịch & Đối soát</h1>
          <p className="mt-2 text-sm text-zinc-400">Theo dõi dòng tiền, callback nhà cung cấp và các tác vụ giao nhận bền vững.</p>
        </div>
        <div className="flex gap-3">
          {activeTab === 'transactions' && (
            <button onClick={() => exportAdminPayments(params)}
              className="flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black uppercase hover:bg-zinc-800">
              <Download className="h-4 w-4 text-emerald-400" /> Xuất CSV
            </button>
          )}
          <button onClick={load} className="flex items-center gap-2 rounded-xl bg-zinc-800 px-4 py-2.5 text-xs font-black uppercase">
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
          </button>
        </div>
      </header>

      <nav className="flex gap-2 overflow-x-auto rounded-2xl border border-zinc-800 bg-zinc-900 p-2">
        {tabs.map(tab => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`rounded-xl px-5 py-2.5 text-xs font-black uppercase transition ${
              activeTab === tab.key ? 'bg-brand-orange text-white' : 'text-zinc-400 hover:bg-zinc-800'
            }`}>
            {tab.label}
          </button>
        ))}
      </nav>

      {activeTab === 'transactions' && (
        <section className="grid gap-3 rounded-2xl border border-zinc-800 bg-zinc-900 p-5 md:grid-cols-4">
          <div className="relative md:col-span-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
            <input value={filters.query} onChange={event => setFilters(current => ({ ...current, query: event.target.value }))}
              placeholder="Mã giao dịch, Booking UUID..."
              className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-brand-orange" />
          </div>
          <select value={filters.status} onChange={event => setFilters(current => ({ ...current, status: event.target.value }))}
            className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm">
            <option value="">Tất cả trạng thái</option>
            {['PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED'].map(value => <option key={value}>{value}</option>)}
          </select>
          <select value={filters.provider} onChange={event => setFilters(current => ({ ...current, provider: event.target.value }))}
            className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm">
            <option value="">Tất cả kênh</option>
            {['VNPAY', 'MOMO', 'CASH', 'MOCK'].map(value => <option key={value}>{value}</option>)}
          </select>
          <select value={filters.reconciliationStatus} onChange={event => setFilters(current => ({ ...current, reconciliationStatus: event.target.value }))}
            className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm">
            <option value="">Tất cả đối soát</option>
            {['NONE', 'REQUIRED', 'IN_REVIEW', 'RESOLVED'].map(value => <option key={value}>{value}</option>)}
          </select>
        </section>
      )}

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <div className="flex items-center justify-between border-b border-zinc-800 px-6 py-4">
          <span className="text-xs font-black uppercase text-zinc-400">{pageInfo.totalElements} bản ghi</span>
          {!isAdmin && <span className="text-xs text-zinc-500">Kế toán: quyền chỉ đọc và xuất báo cáo</span>}
        </div>
        {loading ? (
          <div className="py-20 text-center text-zinc-500">Đang tải dữ liệu...</div>
        ) : items.length === 0 ? (
          <div className="py-20 text-center text-zinc-500">Chưa có dữ liệu phù hợp bộ lọc.</div>
        ) : (
          <div className="overflow-x-auto">
            {activeTab === 'transactions'
              ? <TransactionTable items={items} navigate={navigate} />
              : <OperationTable
                  kind={activeTab}
                  items={items}
                  isAdmin={isAdmin}
                  onReplay={replay}
                  onReconciliationAction={(mode, item) => setReconciliationAction({ mode, item })}
                />}
          </div>
        )}
      </section>
      {reconciliationAction && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <form onSubmit={submitReconciliation} className="w-full max-w-lg rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl">
            <h2 className="text-xl font-black">
              {reconciliationAction.mode === 'assign' ? 'Nhận xử lý hồ sơ' : 'Kết luận đối soát'}
            </h2>
            <p className="mt-2 break-all font-mono text-xs text-zinc-500">{reconciliationAction.item.publicId}</p>
            {reconciliationAction.mode === 'assign' ? (
              <p className="mt-6 rounded-xl bg-zinc-950 p-4 text-sm text-zinc-400">
                Hồ sơ sẽ được giao cho tài khoản quản trị #{accountId}. Mọi thay đổi đều được ghi vết.
              </p>
            ) : (
              <div className="mt-6 space-y-4">
                <label className="block text-xs font-black uppercase text-zinc-400">
                  Mã kết luận
                  <input required maxLength={100} value={resolutionForm.resolutionCode}
                    onChange={event => setResolutionForm(current => ({ ...current, resolutionCode: event.target.value }))}
                    placeholder="Ví dụ: PROVIDER_CONFIRMED"
                    className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange" />
                </label>
                <label className="block text-xs font-black uppercase text-zinc-400">
                  Ghi chú bắt buộc
                  <textarea required maxLength={2000} rows={4} value={resolutionForm.note}
                    onChange={event => setResolutionForm(current => ({ ...current, note: event.target.value }))}
                    className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange" />
                </label>
                <label className="flex items-center gap-3 text-sm text-zinc-300">
                  <input type="checkbox" checked={resolutionForm.ignored}
                    onChange={event => setResolutionForm(current => ({ ...current, ignored: event.target.checked }))} />
                  Đóng hồ sơ theo hướng không cần hành động thêm
                </label>
              </div>
            )}
            <div className="mt-6 flex justify-end gap-3">
              <button type="button" onClick={() => setReconciliationAction(null)}
                className="rounded-xl border border-zinc-700 px-5 py-2.5 text-xs font-black uppercase">Quay lại</button>
              <button disabled={loading} className="rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase disabled:opacity-50">
                {loading ? 'Đang lưu...' : 'Xác nhận'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function TransactionTable({ items, navigate }) {
  return (
    <table className="w-full min-w-[1050px] text-left text-sm">
      <thead className="bg-zinc-950/60 text-[10px] uppercase tracking-wider text-zinc-500">
        <tr><th className="p-4">Giao dịch</th><th>Booking</th><th>Kênh</th><th>Số tiền</th><th>Trạng thái</th><th>Đối soát</th><th>Thời điểm</th><th></th></tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {items.map(item => (
          <tr key={item.paymentPublicId} className="hover:bg-zinc-800/30">
            <td className="p-4"><strong className="block">{item.paymentTransactionCode}</strong><span className="font-mono text-[10px] text-zinc-600">{item.paymentPublicId}</span></td>
            <td className="font-mono text-xs text-zinc-400">{item.bookingPublicId}</td>
            <td><strong>{item.provider}</strong><span className="block text-[10px] text-zinc-500">Lần {item.attemptNumber}</span></td>
            <td className="font-black text-brand-orange">{money(item.amount, item.currency)}</td>
            <td><span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${badge(item.status)}`}>{viStatus[item.status] || item.status}</span></td>
            <td><span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${badge(item.reconciliationStatus)}`}>{viStatus[item.reconciliationStatus] || item.reconciliationStatus}</span></td>
            <td className="text-xs text-zinc-400">{item.createdAt ? new Date(item.createdAt).toLocaleString('vi-VN') : '—'}</td>
            <td><button onClick={() => navigate(`/admin/payments/${item.paymentPublicId}`)} className="rounded-xl border border-zinc-700 p-2 hover:bg-zinc-800" title="Xem chi tiết"><Eye className="h-4 w-4" /></button></td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function OperationTable({ kind, items, isAdmin, onReplay, onReconciliationAction }) {
  return (
    <table className="w-full min-w-[950px] text-left text-sm">
      <thead className="bg-zinc-950/60 text-[10px] uppercase tracking-wider text-zinc-500">
        <tr><th className="p-4">Định danh</th><th>Loại / Nguồn</th><th>Trạng thái</th><th>Thử lại</th><th>Lỗi gần nhất</th><th>Thời điểm</th><th></th></tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {items.map(item => {
          const id = kind === 'webhooks' ? item.id : kind === 'outbox' ? item.eventId : item.publicId;
          const status = item.processingStatus || item.status;
          return (
            <tr key={id} className="hover:bg-zinc-800/30">
              <td className="p-4 font-mono text-xs">{id}</td>
              <td><strong>{item.providerCode || item.destination || item.reasonCode}</strong><span className="block text-[10px] text-zinc-500">{item.eventType || item.sourceReference || ''}</span></td>
              <td><span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${badge(status)}`}>{viStatus[status] || status}</span></td>
              <td>{item.retryCount ?? item.attemptCount ?? '—'}</td>
              <td className="max-w-xs truncate text-xs text-red-300">{item.lastErrorSanitized || item.detailSanitized || '—'}</td>
              <td className="text-xs text-zinc-400">{new Date(item.receivedAt || item.createdAt || item.openedAt).toLocaleString('vi-VN')}</td>
              <td className="p-3">
                {kind === 'reconciliations' ? (
                  <div className="flex gap-2">
                    <span className="rounded-lg border border-zinc-700 p-2" title="Hồ sơ đối soát"><ShieldAlert className="h-4 w-4" /></span>
                    {isAdmin && !['RESOLVED', 'IGNORED'].includes(status) && (
                      <>
                        <button onClick={() => onReconciliationAction('assign', item)}
                          className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase hover:text-brand-orange">Nhận xử lý</button>
                        <button onClick={() => onReconciliationAction('resolve', item)}
                          className="rounded-lg border border-emerald-500/30 px-3 py-2 text-[10px] font-black uppercase text-emerald-400">Kết luận</button>
                      </>
                    )}
                  </div>
                ) : isAdmin && (
                  <button onClick={() => onReplay(kind, id)} className="rounded-lg border border-zinc-700 p-2 hover:text-brand-orange" title="Xử lý lại"><RotateCcw className="h-4 w-4" /></button>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
