import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowRight,
  BadgeDollarSign,
  BarChart3,
  CheckCircle2,
  ClipboardCheck,
  CreditCard,
  Download,
  FileSpreadsheet,
  RefreshCw,
  ShieldCheck,
  WalletCards,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { hasPermissionAccess } from '../permissionAccess';
import { getAnalyticsDashboard } from '@/features/analytics/admin/services/analyticsAdminService';
import {
  exportAdminPayments,
  getAdminRefunds,
  getPaymentOperations,
  searchAdminPayments,
} from '@/features/payment/services/paymentService';
import { getPayrollSummary } from '../services/userAdminService';
import {
  providerLabel,
  statusLabel,
} from '@/features/payment/admin/paymentAdminPresentation';

const money = (value, currency = 'VND') => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: currency || 'VND',
  maximumFractionDigits: 0,
}).format(Number(value) || 0);

const dateTime = value => value
  ? new Date(value).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  : 'Chưa ghi nhận';

const isoDate = date => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const currentMonth = () => isoDate(new Date()).slice(0, 7);

const periodThisMonth = () => {
  const end = new Date();
  const start = new Date(end.getFullYear(), end.getMonth(), 1);
  return { startDate: isoDate(start), endDate: isoDate(end) };
};

const emptyState = {
  analytics: null,
  payments: null,
  reconciliationsOpen: null,
  reconciliationsReview: null,
  refunds: null,
  payroll: null,
};

function MetricCard({ icon: Icon, label, value, hint, tone = 'neutral', onClick }) {
  const tones = {
    neutral: 'border-zinc-800 bg-zinc-900/70 text-zinc-300',
    warning: 'border-amber-500/25 bg-amber-500/[0.06] text-amber-300',
    danger: 'border-red-500/25 bg-red-500/[0.06] text-red-300',
    success: 'border-emerald-500/25 bg-emerald-500/[0.06] text-emerald-300',
  };
  const Component = onClick ? 'button' : 'article';
  return (
    <Component
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      className={`rounded-2xl border p-5 text-left ${tones[tone]} ${onClick ? 'transition hover:border-zinc-600' : ''}`}
    >
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-bold text-zinc-500">{label}</p>
          <p className="mt-3 text-2xl font-black text-white">{value}</p>
          <p className="mt-2 text-xs leading-5 text-zinc-500">{hint}</p>
        </div>
        <span className="rounded-xl border border-current/20 bg-black/20 p-3"><Icon size={20} /></span>
      </div>
    </Component>
  );
}

function WorkflowStep({ number, title, description, action, onClick, disabled }) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="group flex w-full items-start gap-4 rounded-2xl border border-zinc-800 bg-zinc-900/55 p-4 text-left transition hover:border-zinc-600 disabled:cursor-not-allowed disabled:opacity-40"
    >
      <span className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-zinc-800 text-xs font-black text-zinc-300">
        {number}
      </span>
      <span className="min-w-0 flex-1">
        <strong className="text-sm text-zinc-100">{title}</strong>
        <span className="mt-1 block text-xs leading-5 text-zinc-500">{description}</span>
        <span className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-orange-400">
          {action} <ArrowRight size={14} className="transition group-hover:translate-x-0.5" />
        </span>
      </span>
    </button>
  );
}

export default function AdminAccountingWorkspacePage() {
  const navigate = useNavigate();
  const { user, userRole } = useAuth();
  const role = userRole || user?.role;
  const permissions = useMemo(() => user?.permissions || [], [user?.permissions]);
  const can = useCallback(
    (...required) => hasPermissionAccess(role, permissions, ...required),
    [permissions, role],
  );
  const canViewPayments = can('PERM_VIEW_FINANCE', 'PAYMENT_VIEW', 'PAYMENT_RECONCILE');
  const canReconcile = can('PAYMENT_RECONCILE');
  const canViewAnalytics = can('PERM_VIEW_FINANCE', 'ANALYTICS_VIEW');
  const canViewPayroll = can('PAYROLL_VIEW');
  const [data, setData] = useState(emptyState);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [unavailable, setUnavailable] = useState([]);

  const load = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);

    const jobs = [];
    if (canViewAnalytics) {
      jobs.push(['analytics', getAnalyticsDashboard(periodThisMonth()), 'báo cáo doanh thu']);
    }
    if (canViewPayments) {
      jobs.push(['payments', searchAdminPayments({ page: 0, size: 6 }), 'giao dịch gần đây']);
      jobs.push(['reconciliationsOpen', getPaymentOperations('reconciliations', {
        status: 'OPEN', page: 0, size: 1,
      }), 'hồ sơ đối soát mới']);
      jobs.push(['reconciliationsReview', getPaymentOperations('reconciliations', {
        status: 'IN_REVIEW', page: 0, size: 1,
      }), 'hồ sơ đang kiểm tra']);
      jobs.push(['refunds', getAdminRefunds({
        status: 'REQUIRES_ACTION', page: 0, size: 1,
      }), 'khoản hoàn cần chú ý']);
    }
    if (canViewPayroll) {
      jobs.push(['payroll', getPayrollSummary(currentMonth()), 'kỳ lương']);
    }

    const results = await Promise.allSettled(jobs.map(([, request]) => request));
    const next = { ...emptyState };
    const failed = [];
    results.forEach((result, index) => {
      const [key, , label] = jobs[index];
      if (result.status === 'fulfilled') next[key] = result.value;
      else failed.push(label);
    });
    setData(next);
    setUnavailable(failed);
    setLoading(false);
    setRefreshing(false);
  }, [canViewAnalytics, canViewPayroll, canViewPayments]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const summary = data.analytics?.summary || {};
  const openReconciliations = Number(data.reconciliationsOpen?.totalElements) || 0;
  const reviewingReconciliations = Number(data.reconciliationsReview?.totalElements) || 0;
  const reconciliationTotal = openReconciliations + reviewingReconciliations;
  const payrollPending = Number(data.payroll?.pendingApproval || 0)
    + Number(data.payroll?.approved || 0)
    + Number(data.payroll?.paymentPending || 0);
  const payments = useMemo(() => data.payments?.content || [], [data.payments]);

  if (loading) {
    return (
      <div className="grid min-h-[60vh] place-items-center text-sm text-zinc-500">
        Đang tổng hợp công việc kế toán...
      </div>
    );
  }

  return (
    <main className="mx-auto w-full max-w-[1500px] space-y-6 pb-16 text-white">
      <header className="flex flex-col justify-between gap-5 border-b border-zinc-800 pb-6 xl:flex-row xl:items-end">
        <div>
          <p className="flex items-center gap-2 text-xs font-black uppercase tracking-[0.2em] text-orange-400">
            <BadgeDollarSign size={17} /> Kế toán vận hành
          </p>
          <h1 className="mt-2 text-3xl font-black">Bàn làm việc kế toán</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            Một điểm bắt đầu cho doanh thu, giao dịch lệch, hoàn tiền và kỳ lương. Ưu tiên xử lý
            hồ sơ có chênh lệch trước, sau đó mới xuất số liệu bàn giao.
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          {canViewPayments && (
            <button
              type="button"
              onClick={() => exportAdminPayments({})}
              className="inline-flex items-center gap-2 rounded-xl border border-zinc-700 px-4 py-2.5 text-xs font-black uppercase text-zinc-200 hover:bg-zinc-800"
            >
              <Download size={16} /> Xuất giao dịch
            </button>
          )}
          <button
            type="button"
            disabled={refreshing}
            onClick={() => load(true)}
            className="inline-flex items-center gap-2 rounded-xl bg-zinc-100 px-4 py-2.5 text-xs font-black uppercase text-zinc-950 disabled:opacity-50"
          >
            <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} /> Làm mới
          </button>
        </div>
      </header>

      <section className="rounded-2xl border border-sky-500/20 bg-sky-500/[0.05] px-5 py-4 text-sm leading-6 text-sky-100/75">
        <p className="flex items-center gap-2 font-black text-sky-200">
          <ShieldCheck size={17} /> Phạm vi trách nhiệm
        </p>
        <p className="mt-1">
          Đây là kế toán vận hành rạp: kiểm tra doanh thu, đối soát chênh lệch thanh toán và xử lý
          bảng lương. Hoàn tiền cho khách, phát lại webhook và sửa trạng thái kỹ thuật vẫn thuộc
          quản trị viên hoặc quy trình đã được phê duyệt.
        </p>
      </section>

      {unavailable.length > 0 && (
        <section className="flex items-start gap-3 rounded-2xl border border-amber-500/25 bg-amber-500/[0.06] px-5 py-4 text-sm text-amber-100/80">
          <AlertTriangle className="mt-0.5 shrink-0 text-amber-400" size={18} />
          <p>
            Một phần số liệu đang tạm thời chưa tải được: {unavailable.join(', ')}. Các khu vực còn
            lại vẫn có thể tiếp tục làm việc.
          </p>
        </section>
      )}

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={BarChart3}
          label="Doanh thu thuần tháng này"
          value={canViewAnalytics ? money(summary.netRevenue, summary.currency) : 'Không được cấp quyền'}
          hint={canViewAnalytics
            ? `Doanh thu gộp ${money(summary.grossRevenue, summary.currency)}`
            : 'Cần quyền xem báo cáo doanh thu'}
          onClick={canViewAnalytics ? () => navigate('/admin/analytics') : undefined}
        />
        <MetricCard
          icon={ClipboardCheck}
          label="Hồ sơ cần đối soát"
          value={canViewPayments ? reconciliationTotal : '—'}
          hint={`${openReconciliations} hồ sơ mới · ${reviewingReconciliations} đang kiểm tra`}
          tone={reconciliationTotal ? 'danger' : 'success'}
          onClick={canViewPayments ? () => navigate('/admin/payments?tab=reconciliations') : undefined}
        />
        <MetricCard
          icon={WalletCards}
          label="Việc kỳ lương chưa hoàn tất"
          value={canViewPayroll ? payrollPending : '—'}
          hint={canViewPayroll
            ? `${Number(data.payroll?.paymentPending) || 0} phiếu đang chờ khớp chứng từ`
            : 'Không thuộc phạm vi được cấp'}
          tone={payrollPending ? 'warning' : 'success'}
          onClick={canViewPayroll ? () => navigate('/admin/payroll') : undefined}
        />
        <MetricCard
          icon={CreditCard}
          label="Khoản hoàn cần chú ý"
          value={canViewPayments ? Number(data.refunds?.totalElements) || 0 : '—'}
          hint="Kế toán theo dõi số tiền; quyết định hoàn theo đúng luồng phê duyệt"
          tone={Number(data.refunds?.totalElements) ? 'warning' : 'neutral'}
          onClick={canViewPayments ? () => navigate('/admin/payments?tab=refunds') : undefined}
        />
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.4fr]">
        <div className="space-y-4">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.16em] text-zinc-600">Một vòng làm việc</p>
            <h2 className="mt-2 text-xl font-black">Làm theo thứ tự này</h2>
          </div>
          <WorkflowStep
            number="01"
            title="Nắm số doanh thu"
            description="Xem doanh thu thuần, khoản hoàn và phạm vi dữ liệu trước khi bắt đầu đối soát."
            action="Mở báo cáo"
            disabled={!canViewAnalytics}
            onClick={() => navigate('/admin/analytics')}
          />
          <WorkflowStep
            number="02"
            title="Xử lý giao dịch chênh lệch"
            description={canReconcile
              ? 'Tiếp nhận hồ sơ, kiểm tra mã nhà cung cấp và ghi kết luận có căn cứ.'
              : 'Theo dõi hồ sơ lệch và chuyển người có quyền đối soát.'}
            action={canReconcile ? 'Vào hàng đợi đối soát' : 'Xem giao dịch'}
            disabled={!canViewPayments}
            onClick={() => navigate('/admin/payments?tab=reconciliations')}
          />
          <WorkflowStep
            number="03"
            title="Chốt quy trình bảng lương"
            description="Kiểm tra phiếu, duyệt độc lập, gửi lô ngân hàng và khớp mã bút toán."
            action="Mở kỳ lương"
            disabled={!canViewPayroll}
            onClick={() => navigate('/admin/payroll')}
          />
          <WorkflowStep
            number="04"
            title="Xuất số liệu bàn giao"
            description="Xuất danh sách giao dịch sau khi các hồ sơ lệch đã có kết luận."
            action="Xuất CSV"
            disabled={!canViewPayments}
            onClick={() => exportAdminPayments({})}
          />
        </div>

        <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/60">
          <header className="flex items-center justify-between gap-4 border-b border-zinc-800 px-5 py-4">
            <div>
              <h2 className="flex items-center gap-2 text-sm font-black text-zinc-100">
                <FileSpreadsheet size={17} className="text-orange-400" /> Giao dịch gần đây
              </h2>
              <p className="mt-1 text-xs text-zinc-500">Dùng để phát hiện nhanh khoản tiền vừa phát sinh bất thường.</p>
            </div>
            {canViewPayments && (
              <button
                type="button"
                onClick={() => navigate('/admin/payments')}
                className="text-xs font-bold text-orange-400 hover:text-orange-300"
              >
                Xem tất cả
              </button>
            )}
          </header>
          {!canViewPayments ? (
            <p className="px-5 py-16 text-center text-sm text-zinc-600">Không có quyền xem giao dịch.</p>
          ) : payments.length === 0 ? (
            <div className="px-5 py-16 text-center">
              <CheckCircle2 className="mx-auto text-emerald-500" size={28} />
              <p className="mt-3 text-sm font-bold text-zinc-300">Chưa có giao dịch trong phạm vi hiện tại</p>
            </div>
          ) : (
            <div className="divide-y divide-zinc-800">
              {payments.map(payment => (
                <button
                  key={payment.paymentPublicId}
                  type="button"
                  onClick={() => navigate(`/admin/payments/${payment.paymentPublicId}`)}
                  className="grid w-full gap-3 px-5 py-4 text-left transition hover:bg-white/[0.025] sm:grid-cols-[1fr_auto_auto] sm:items-center"
                >
                  <span className="min-w-0">
                    <strong className="block truncate text-sm text-zinc-200">
                      {payment.movieTitle || payment.paymentTransactionCode || 'Giao dịch thanh toán'}
                    </strong>
                    <span className="mt-1 block text-xs text-zinc-600">
                      {providerLabel(payment.provider)} · {dateTime(payment.createdAt)}
                    </span>
                  </span>
                  <span className="text-sm font-black text-zinc-200">
                    {money(payment.amount, payment.currency)}
                  </span>
                  <span className={`text-xs font-bold ${
                    ['REQUIRED', 'IN_REVIEW'].includes(payment.reconciliationStatus)
                      ? 'text-amber-300'
                      : 'text-zinc-500'
                  }`}>
                    {['REQUIRED', 'IN_REVIEW'].includes(payment.reconciliationStatus)
                      ? 'Cần đối soát'
                      : statusLabel(payment.status)}
                  </span>
                </button>
              ))}
            </div>
          )}
        </section>
      </section>
    </main>
  );
}
