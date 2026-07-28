import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext, useSearchParams } from 'react-router-dom';
import {
  ChevronLeft,
  ChevronRight,
  Download,
  Eye,
  RefreshCw,
  RotateCcw,
  Search,
  ShieldCheck,
  Wrench,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import {
  assignReconciliation,
  exportAdminPayments,
  getPaymentOperations,
  paymentErrorMessage,
  replayPaymentOperation,
  resolveReconciliation,
  searchAdminPayments,
} from '../../services/paymentService';
import {
  OPERATION_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
  PROVIDER_LABELS,
  RECONCILIATION_STATUS_LABELS,
  RESOLUTION_OPTIONS,
  destinationLabel,
  humanizeSystemMessage,
  paymentActionLabel,
  providerLabel,
  reasonLabel,
  statusLabel,
} from '../paymentAdminPresentation';

const BUSINESS_TABS = [
  { key: 'transactions', label: 'Giao dịch thanh toán' },
  { key: 'reconciliations', label: 'Cần xử lý' },
];

const TECHNICAL_TABS = [
  { key: 'webhooks', label: 'Thông báo nhà cung cấp' },
  { key: 'outbox', label: 'Hàng đợi hệ thống' },
];

const money = (value, currency = 'VND') =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency }).format(Number(value || 0));

const formatTime = value => value
  ? new Date(value).toLocaleString('vi-VN')
  : 'Chưa ghi nhận';

const badge = status => {
  if (['SUCCESS', 'PUBLISHED', 'PROCESSED', 'RESOLVED'].includes(status)) {
    return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-400';
  }
  if (['FAILED', 'CANCELLED', 'EXPIRED', 'DEAD_LETTER'].includes(status)) {
    return 'border-red-500/30 bg-red-500/10 text-red-400';
  }
  if (status === 'NONE') {
    return 'border-zinc-700 bg-zinc-800/70 text-zinc-300';
  }
  return 'border-amber-500/30 bg-amber-500/10 text-amber-400';
};

const operationDescription = item => {
  const detail = item.lastErrorSanitized || item.detailSanitized;
  if (detail) {
    const translatedReason = reasonLabel(detail);
    return translatedReason === detail
      ? humanizeSystemMessage(detail)
      : translatedReason;
  }
  if (item.destination) return `Chuyển dữ liệu đến ${destinationLabel(item.destination).toLowerCase()}.`;
  if (item.providerCode) return `Thông báo do ${providerLabel(item.providerCode)} gửi về.`;
  return 'Không có ghi chú lỗi.';
};

export default function AdminPaymentsPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { userRole, accountId } = useAuth();
  const { triggerToast, triggerConfirm, triggerAlert } = useOutletContext() || {};
  const isAdmin = (userRole || '').replace(/^ROLE_/, '') === 'ADMIN';
  const [activeTab, setActiveTab] = useState('transactions');
  const [technicalOpen, setTechnicalOpen] = useState(false);
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ number: 0, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({
    query: searchParams.get('query') || '',
    status: '',
    provider: '',
    reconciliationStatus: '',
  });
  const [reconciliationAction, setReconciliationAction] = useState(null);
  const [resolutionForm, setResolutionForm] = useState({
    resolutionCode: '',
    note: '',
  });

  const params = useMemo(() => ({
    page,
    size: 20,
    ...(filters.query && { query: filters.query.trim() }),
    ...(filters.status && { status: filters.status }),
    ...(filters.provider && { provider: filters.provider }),
    ...(filters.reconciliationStatus && {
      reconciliationStatus: filters.reconciliationStatus,
    }),
  }), [filters, page]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const result = activeTab === 'transactions'
        ? await searchAdminPayments(params)
        : await getPaymentOperations(activeTab, { page, size: 20 });
      setItems(result?.content || []);
      setPageInfo({
        number: result?.number || 0,
        totalPages: result?.totalPages || 0,
        totalElements: result?.totalElements || 0,
      });
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [activeTab, page, params, triggerAlert]);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const changeTab = key => {
    setActiveTab(key);
    setPage(0);
  };

  const updateFilter = (field, value) => {
    setFilters(current => ({ ...current, [field]: value }));
    setPage(0);
  };

  const resetFilters = () => {
    setFilters({ query: '', status: '', provider: '', reconciliationStatus: '' });
    setPage(0);
  };

  const replay = async (kind, id) => {
    if (!isAdmin) return;
    const accepted = triggerConfirm
      ? await triggerConfirm(
          'Đưa tác vụ lỗi này vào hàng đợi xử lý lại? Hệ thống vẫn giữ nguyên mã chống xử lý trùng.',
        )
      : true;
    if (!accepted) return;
    try {
      await replayPaymentOperation(kind, id);
      triggerToast?.('Đã đưa tác vụ vào hàng đợi xử lý lại.');
      await load();
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
        triggerToast?.('Bạn đã tiếp nhận hồ sơ cần kiểm tra.');
      } else {
        await resolveReconciliation(reconciliationAction.item.publicId, {
          ...resolutionForm,
          ignored: resolutionForm.resolutionCode === 'NO_ACTION_REQUIRED',
        });
        triggerToast?.('Đã lưu kết luận và đóng hồ sơ.');
      }
      setReconciliationAction(null);
      setResolutionForm({ resolutionCode: '', note: '' });
      await load();
    } catch (error) {
      triggerAlert?.(paymentErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const selectedResolution = RESOLUTION_OPTIONS.find(
    option => option.value === resolutionForm.resolutionCode,
  );

  return (
    <div className="mx-auto w-full max-w-[1600px] space-y-6 text-white">
      <header className="flex flex-col justify-between gap-4 border-b border-zinc-800 pb-6 lg:flex-row lg:items-end">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.22em] text-brand-orange">
            Vận hành thanh toán
          </p>
          <h1 className="mt-2 text-3xl font-black uppercase">Giao dịch & đối soát</h1>
          <p className="mt-2 max-w-3xl text-sm text-zinc-400">
            Theo dõi tiền khách đã thanh toán, kiểm tra giao dịch bất thường và bảo đảm kết quả
            được chuyển chính xác sang đơn đặt vé.
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          {activeTab === 'transactions' && (
            <button
              onClick={() => exportAdminPayments(params)}
              className="flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black uppercase hover:bg-zinc-800"
            >
              <Download className="h-4 w-4 text-emerald-400" /> Xuất danh sách
            </button>
          )}
          <button
            onClick={load}
            className="flex items-center gap-2 rounded-xl bg-zinc-800 px-4 py-2.5 text-xs font-black uppercase"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
          </button>
        </div>
      </header>

      <div className="grid gap-4 md:grid-cols-3">
        <QuickCard
          title="Tất cả giao dịch"
          description="Tra cứu mọi lần khách thanh toán"
          active={activeTab === 'transactions' && !filters.status && !filters.reconciliationStatus}
          onClick={() => {
            changeTab('transactions');
            resetFilters();
          }}
        />
        <QuickCard
          title="Đang chờ kết quả"
          description="Không thu lại tiền khi giao dịch còn hoạt động"
          tone="warning"
          active={activeTab === 'transactions' && filters.status === 'PROCESSING'}
          onClick={() => {
            changeTab('transactions');
            setFilters(current => ({
              ...current,
              status: 'PROCESSING',
              reconciliationStatus: '',
            }));
          }}
        />
        <QuickCard
          title="Cần nhân viên kiểm tra"
          description="Kết quả thanh toán và đơn đặt vé chưa đồng nhất"
          tone="danger"
          active={activeTab === 'reconciliations'}
          onClick={() => changeTab('reconciliations')}
        />
      </div>

      <div className="rounded-2xl border border-zinc-800 bg-zinc-900 p-2">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <nav className="flex flex-wrap gap-2">
            {BUSINESS_TABS.map(tab => (
              <TabButton
                key={tab.key}
                tab={tab}
                activeTab={activeTab}
                onClick={() => changeTab(tab.key)}
              />
            ))}
          </nav>
          <button
            type="button"
            onClick={() => setTechnicalOpen(value => !value)}
            className={`flex items-center gap-2 rounded-xl px-4 py-2.5 text-xs font-black uppercase transition ${
              technicalOpen
                ? 'bg-zinc-700 text-white'
                : 'text-zinc-400 hover:bg-zinc-800 hover:text-white'
            }`}
          >
            <Wrench className="h-4 w-4" />
            Công cụ kỹ thuật
          </button>
        </div>
        {technicalOpen && (
          <div className="mt-2 flex flex-wrap gap-2 border-t border-zinc-800 pt-2">
            <p className="w-full px-3 pb-1 text-xs text-zinc-500">
              Chỉ dùng khi điều tra callback hoặc tác vụ giao nhận bị lỗi.
            </p>
            {TECHNICAL_TABS.map(tab => (
              <TabButton
                key={tab.key}
                tab={tab}
                activeTab={activeTab}
                onClick={() => changeTab(tab.key)}
              />
            ))}
          </div>
        )}
      </div>

      {activeTab === 'transactions' && (
        <section className="rounded-2xl border border-zinc-800 bg-zinc-900 p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-black uppercase">Tìm giao dịch</h2>
              <p className="mt-1 text-xs text-zinc-500">
                Có thể tìm bằng mã giao dịch, mã tham chiếu hoặc mã đơn liên kết.
              </p>
            </div>
            <button
              type="button"
              onClick={resetFilters}
              className="text-xs font-bold text-zinc-400 hover:text-white"
            >
              Xóa bộ lọc
            </button>
          </div>
          <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-500" />
              <input
                value={filters.query}
                onChange={event => updateFilter('query', event.target.value)}
                placeholder="Mã giao dịch hoặc mã đơn..."
                className="w-full rounded-xl border border-zinc-700 bg-zinc-950 py-2.5 pl-10 pr-3 text-sm outline-none focus:border-brand-orange"
              />
            </div>
            <select
              aria-label="Trạng thái giao dịch"
              value={filters.status}
              onChange={event => updateFilter('status', event.target.value)}
              className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm"
            >
              <option value="">Mọi kết quả thanh toán</option>
              {Object.entries(PAYMENT_STATUS_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
            <select
              aria-label="Kênh thanh toán"
              value={filters.provider}
              onChange={event => updateFilter('provider', event.target.value)}
              className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm"
            >
              <option value="">Mọi kênh thanh toán</option>
              {Object.entries(PROVIDER_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
            <select
              aria-label="Trạng thái kiểm tra"
              value={filters.reconciliationStatus}
              onChange={event => updateFilter('reconciliationStatus', event.target.value)}
              className="rounded-xl border border-zinc-700 bg-zinc-950 px-3 text-sm"
            >
              <option value="">Mọi trạng thái kiểm tra</option>
              {Object.entries(RECONCILIATION_STATUS_LABELS)
                .filter(([value]) => ['NONE', 'REQUIRED', 'IN_REVIEW', 'RESOLVED'].includes(value))
                .map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
            </select>
          </div>
        </section>
      )}

      <section className="overflow-hidden rounded-3xl border border-zinc-800 bg-zinc-900">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-zinc-800 px-6 py-4">
          <div>
            <span className="text-xs font-black uppercase text-zinc-300">
              {pageInfo.totalElements} bản ghi
            </span>
            <p className="mt-1 text-xs text-zinc-500">
              {activeTab === 'transactions' && 'Mỗi dòng là một lần thử thanh toán, không phải một đơn đặt vé.'}
              {activeTab === 'reconciliations' && 'Chỉ kết luận sau khi đã kiểm tra nhà cung cấp và trạng thái đơn.'}
              {activeTab === 'webhooks' && 'Thông báo máy chủ nhận trực tiếp từ cổng thanh toán.'}
              {activeTab === 'outbox' && 'Tác vụ chuyển kết quả sang Booking và hệ thống báo cáo.'}
            </p>
          </div>
          {!isAdmin && (
            <span className="text-xs text-zinc-500">
              Kế toán có quyền xem và xuất danh sách, không có quyền xử lý lại.
            </span>
          )}
        </div>
        {loading ? (
          <div className="py-20 text-center text-zinc-500">Đang tải dữ liệu...</div>
        ) : items.length === 0 ? (
          <EmptyState activeTab={activeTab} />
        ) : (
          <div className="overflow-x-auto">
            {activeTab === 'transactions'
              ? <TransactionTable items={items} navigate={navigate} />
              : (
                <OperationTable
                  kind={activeTab}
                  items={items}
                  isAdmin={isAdmin}
                  onReplay={replay}
                  onReconciliationAction={(mode, item) => {
                    setResolutionForm({ resolutionCode: '', note: '' });
                    setReconciliationAction({ mode, item });
                  }}
                />
              )}
          </div>
        )}
        {pageInfo.totalPages > 1 && (
          <Pagination
            page={pageInfo.number}
            totalPages={pageInfo.totalPages}
            onChange={setPage}
          />
        )}
      </section>

      {reconciliationAction && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
          <form
            onSubmit={submitReconciliation}
            className="w-full max-w-lg rounded-3xl border border-zinc-700 bg-zinc-900 p-6 shadow-2xl"
          >
            <h2 className="text-xl font-black">
              {reconciliationAction.mode === 'assign'
                ? 'Tiếp nhận hồ sơ kiểm tra'
                : 'Ghi nhận kết luận'}
            </h2>
            <p className="mt-2 text-sm text-zinc-400">
              {reasonLabel(reconciliationAction.item.reasonCode)}
            </p>
            {reconciliationAction.mode === 'assign' ? (
              <div className="mt-6 rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4">
                <p className="text-sm font-bold text-amber-300">Trách nhiệm sau khi tiếp nhận</p>
                <p className="mt-2 text-sm leading-6 text-zinc-400">
                  Hồ sơ sẽ được giao cho tài khoản đang đăng nhập. Hãy kiểm tra giao dịch trên
                  cổng thanh toán, mở đơn đặt vé liên quan và ghi kết luận có căn cứ.
                </p>
              </div>
            ) : (
              <div className="mt-6 space-y-4">
                <label className="block text-xs font-black uppercase text-zinc-400">
                  Kết quả kiểm tra
                  <select
                    required
                    value={resolutionForm.resolutionCode}
                    onChange={event => setResolutionForm(current => ({
                      ...current,
                      resolutionCode: event.target.value,
                    }))}
                    className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange"
                  >
                    <option value="">Chọn kết quả đã xác minh...</option>
                    {RESOLUTION_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                {selectedResolution && (
                  <p className="rounded-xl bg-zinc-950 p-3 text-xs leading-5 text-zinc-400">
                    {selectedResolution.help}
                  </p>
                )}
                <label className="block text-xs font-black uppercase text-zinc-400">
                  Bằng chứng và ghi chú
                  <textarea
                    required
                    maxLength={2000}
                    rows={4}
                    value={resolutionForm.note}
                    onChange={event => setResolutionForm(current => ({
                      ...current,
                      note: event.target.value,
                    }))}
                    placeholder="Ghi rõ đã kiểm tra ở đâu, mã tham chiếu và kết quả đối chiếu..."
                    className="mt-2 w-full rounded-xl border border-zinc-700 bg-zinc-950 p-3 text-sm normal-case text-white outline-none focus:border-brand-orange"
                  />
                </label>
              </div>
            )}
            <details className="mt-4 text-xs text-zinc-600">
              <summary className="cursor-pointer">Mã hồ sơ kỹ thuật</summary>
              <p className="mt-2 break-all font-mono">{reconciliationAction.item.publicId}</p>
            </details>
            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setReconciliationAction(null)}
                className="rounded-xl border border-zinc-700 px-5 py-2.5 text-xs font-black uppercase"
              >
                Quay lại
              </button>
              <button
                disabled={loading}
                className="rounded-xl bg-brand-orange px-5 py-2.5 text-xs font-black uppercase disabled:opacity-50"
              >
                {loading ? 'Đang lưu...' : 'Xác nhận'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function QuickCard({ title, description, active, tone = 'default', onClick }) {
  const toneClass = {
    default: 'text-white',
    warning: 'text-amber-300',
    danger: 'text-red-300',
  }[tone];
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-2xl border p-5 text-left transition ${
        active
          ? 'border-brand-orange bg-brand-orange/10'
          : 'border-zinc-800 bg-zinc-900 hover:border-zinc-700'
      }`}
    >
      <strong className={`block text-sm ${toneClass}`}>{title}</strong>
      <span className="mt-2 block text-xs leading-5 text-zinc-500">{description}</span>
    </button>
  );
}

function TabButton({ tab, activeTab, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-xl px-5 py-2.5 text-xs font-black uppercase transition ${
        activeTab === tab.key
          ? 'bg-brand-orange text-white'
          : 'text-zinc-400 hover:bg-zinc-800'
      }`}
    >
      {tab.label}
    </button>
  );
}

function EmptyState({ activeTab }) {
  const copy = {
    transactions: ['Không có giao dịch phù hợp', 'Hãy đổi bộ lọc hoặc kiểm tra lại mã cần tìm.'],
    reconciliations: ['Không có hồ sơ cần xử lý', 'Các giao dịch hiện không có chênh lệch cần nhân viên kiểm tra.'],
    webhooks: ['Chưa có thông báo nhà cung cấp', 'Đây là trạng thái bình thường khi chưa có callback phù hợp.'],
    outbox: ['Chưa có tác vụ giao nhận', 'Không có tác vụ hệ thống phù hợp với trang hiện tại.'],
  }[activeTab];
  return (
    <div className="flex flex-col items-center py-20 text-center">
      <ShieldCheck className="h-9 w-9 text-zinc-700" />
      <strong className="mt-4 text-zinc-300">{copy[0]}</strong>
      <p className="mt-2 text-sm text-zinc-600">{copy[1]}</p>
    </div>
  );
}

function TransactionTable({ items, navigate }) {
  return (
    <table className="w-full min-w-[1180px] text-left text-sm">
      <thead className="bg-zinc-950/60 text-[10px] uppercase tracking-wider text-zinc-500">
        <tr>
          <th className="p-4">Giao dịch</th>
          <th>Đơn / phim</th>
          <th>Kênh</th>
          <th>Số tiền</th>
          <th>Kết quả</th>
          <th>Việc cần làm</th>
          <th>Thời điểm</th>
          <th>Hành động</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {items.map(item => (
          <tr key={item.paymentPublicId} className="hover:bg-zinc-800/30">
            <td className="p-4">
              <strong className="block">{item.paymentTransactionCode}</strong>
              <span className="mt-1 block text-[10px] text-zinc-500">
                Lần thanh toán thứ {item.attemptNumber}
              </span>
            </td>
            <td>
              <strong className="block max-w-[230px] truncate">
                {item.movieTitle || 'Đơn đặt vé liên kết'}
              </strong>
              <button
                type="button"
                onClick={() => navigate(`/admin/bookings/${item.bookingPublicId}`)}
                className="mt-1 text-xs font-bold text-brand-orange hover:underline"
              >
                Mở đơn đặt vé
              </button>
              {item.ticketCount != null && (
                <span className="ml-2 text-[10px] text-zinc-500">{item.ticketCount} vé</span>
              )}
            </td>
            <td>
              <strong>{providerLabel(item.provider)}</strong>
              <span className="block text-[10px] text-zinc-500">
                {item.paymentMethod === 'CASH' ? 'Tại quầy' : 'Trực tuyến'}
              </span>
            </td>
            <td className="font-black text-brand-orange">{money(item.amount, item.currency)}</td>
            <td>
              <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${badge(item.status)}`}>
                {statusLabel(item.status)}
              </span>
            </td>
            <td>
              <span className={`block max-w-[190px] text-xs font-bold ${
                ['REQUIRED', 'IN_REVIEW'].includes(item.reconciliationStatus)
                  ? 'text-amber-300'
                  : 'text-zinc-400'
              }`}>
                {paymentActionLabel(item)}
              </span>
            </td>
            <td className="text-xs text-zinc-400">{formatTime(item.createdAt)}</td>
            <td>
              <button
                onClick={() => navigate(`/admin/payments/${item.paymentPublicId}`)}
                className="flex items-center gap-2 rounded-xl border border-zinc-700 px-3 py-2 text-xs font-bold hover:bg-zinc-800"
                title="Xem chi tiết giao dịch"
              >
                <Eye className="h-4 w-4" /> Xem
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function OperationTable({
  kind,
  items,
  isAdmin,
  onReplay,
  onReconciliationAction,
}) {
  return (
    <table className="w-full min-w-[1020px] text-left text-sm">
      <thead className="bg-zinc-950/60 text-[10px] uppercase tracking-wider text-zinc-500">
        <tr>
          <th className="p-4">{kind === 'reconciliations' ? 'Vấn đề cần kiểm tra' : 'Nguồn tác vụ'}</th>
          <th>Trạng thái</th>
          <th>Mô tả</th>
          <th>Thử lại</th>
          <th>Thời điểm</th>
          <th>Hành động</th>
        </tr>
      </thead>
      <tbody className="divide-y divide-zinc-800">
        {items.map(item => {
          const id = kind === 'webhooks' ? item.id : kind === 'outbox' ? item.eventId : item.publicId;
          const status = item.processingStatus || item.status;
          const canReplay = kind === 'webhooks'
            ? status === 'FAILED'
            : ['FAILED', 'DEAD_LETTER'].includes(status);
          return (
            <tr key={id} className="hover:bg-zinc-800/30">
              <td className="p-4">
                <strong className="block">
                  {kind === 'reconciliations'
                    ? reasonLabel(item.reasonCode)
                    : item.providerCode
                      ? providerLabel(item.providerCode)
                      : destinationLabel(item.destination)}
                </strong>
                <details className="mt-2 text-[10px] text-zinc-600">
                  <summary className="cursor-pointer">Thông tin kỹ thuật</summary>
                  <p className="mt-1 max-w-xs break-all font-mono">{id}</p>
                  <p>{item.eventType || item.sourceReference || ''}</p>
                </details>
              </td>
              <td>
                <span className={`rounded-full border px-2.5 py-1 text-[10px] font-black ${badge(status)}`}>
                  {OPERATION_STATUS_LABELS[status] || status}
                </span>
              </td>
              <td className="max-w-sm text-xs leading-5 text-zinc-400">
                {operationDescription(item)}
              </td>
              <td>{item.retryCount ?? item.attemptCount ?? '—'}</td>
              <td className="text-xs text-zinc-400">
                {formatTime(item.receivedAt || item.createdAt || item.openedAt)}
              </td>
              <td className="p-3">
                {kind === 'reconciliations' ? (
                  <div className="flex gap-2">
                    {isAdmin && !['RESOLVED', 'IGNORED'].includes(status) && (
                      <>
                        {status !== 'IN_REVIEW' && (
                          <button
                            onClick={() => onReconciliationAction('assign', item)}
                            className="rounded-lg border border-zinc-700 px-3 py-2 text-[10px] font-black uppercase hover:text-brand-orange"
                          >
                            Tiếp nhận
                          </button>
                        )}
                        <button
                          onClick={() => onReconciliationAction('resolve', item)}
                          className="rounded-lg border border-emerald-500/30 px-3 py-2 text-[10px] font-black uppercase text-emerald-400"
                        >
                          Ghi kết luận
                        </button>
                      </>
                    )}
                    {!isAdmin && (
                      <span className="text-xs text-zinc-500">Chỉ xem</span>
                    )}
                  </div>
                ) : isAdmin && canReplay ? (
                  <button
                    onClick={() => onReplay(kind, id)}
                    className="flex items-center gap-2 rounded-lg border border-zinc-700 px-3 py-2 text-xs hover:text-brand-orange"
                    title="Xử lý lại tác vụ lỗi"
                  >
                    <RotateCcw className="h-4 w-4" /> Xử lý lại
                  </button>
                ) : (
                  <span className="text-xs text-zinc-600">
                    {canReplay ? 'Chỉ Admin được xử lý' : 'Không cần thao tác'}
                  </span>
                )}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

function Pagination({ page, totalPages, onChange }) {
  return (
    <div className="flex items-center justify-between border-t border-zinc-800 px-6 py-4">
      <span className="text-xs text-zinc-500">
        Trang {page + 1} / {totalPages}
      </span>
      <div className="flex gap-2">
        <button
          type="button"
          aria-label="Trang trước"
          disabled={page === 0}
          onClick={() => onChange(page - 1)}
          className="rounded-lg border border-zinc-700 p-2 disabled:opacity-30"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <button
          type="button"
          aria-label="Trang sau"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
          className="rounded-lg border border-zinc-700 p-2 disabled:opacity-30"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
