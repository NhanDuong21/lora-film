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
  CalendarClock,
  Landmark,
  RefreshCw,
  Scale,
  ShieldCheck,
  WalletCards,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { getAccountingWorkspaceMode, hasPermissionAccess } from '../permissionAccess';
import { getAnalyticsDashboard } from '@/features/analytics/admin/services/analyticsAdminService';
import {
  exportAdminPayments,
  getAccountingOverview,
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
  accounting: null,
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

export default function AdminAccountingWorkspacePage({ mode }) {
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
  const workspaceMode = mode || getAccountingWorkspaceMode(permissions);
  const isAccountingController = workspaceMode === 'control';
  const canViewAccountingOperations = can(
    'SETTLEMENT_IMPORT',
    'SETTLEMENT_LOCK',
    'CASH_CLOSE_VERIFY',
    'ACCOUNTING_PERIOD_VIEW',
    'AUDIT_VIEW',
  );
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
      jobs.push(['payments', searchAdminPayments({
        reconciliationStatus: 'REQUIRED', page: 0, size: 6,
      }), 'giao dịch cần kiểm tra']);
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
    if (canViewAccountingOperations) {
      jobs.push(['accounting', getAccountingOverview({}), 'bàn kiểm soát kế toán']);
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
  }, [canViewAccountingOperations, canViewAnalytics, canViewPayroll, canViewPayments]);

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
  const workspaceCopy = isAccountingController
    ? {
        eyebrow: 'Kế toán kiểm soát',
        title: 'Bàn kiểm soát kế toán',
        description: 'Tập trung vào các hồ sơ đang chờ quyết định độc lập. Rà soát đủ căn cứ trước khi duyệt hoặc khóa; không nhập liệu thay cho người vận hành.',
        responsibility: 'Bạn là người kiểm soát độc lập: duyệt lương, duyệt hoàn tiền, khóa lô và khóa kỳ sau khi đã kiểm tra chứng từ. Bạn không thể duyệt hồ sơ do chính mình lập hoặc phiếu lương của chính mình.',
        cycleLabel: 'Quy trình kiểm soát',
        cycleTitle: 'Xử lý hàng đợi theo mức rủi ro',
      }
    : {
        eyebrow: 'Kế toán vận hành',
        title: 'Bàn vận hành kế toán',
        description: 'Chuẩn bị, đối chiếu và hoàn thiện chứng từ trước khi chuyển người kiểm soát độc lập. Màn hình ưu tiên các hồ sơ cần xử lý trong ngày.',
        responsibility: 'Bạn chịu trách nhiệm chuẩn bị và đối chiếu số liệu. Những bước rủi ro cao như duyệt hoàn tiền, duyệt lương, khóa lô và khóa kỳ sẽ được chuyển sang kế toán kiểm soát; nút không đủ quyền luôn bị khóa và nêu rõ lý do.',
        cycleLabel: 'Quy trình vận hành',
        cycleTitle: 'Hoàn thiện hồ sơ theo thứ tự',
      };
  const workflowSteps = isAccountingController
    ? [
        {
          title: 'Duyệt đề nghị hoàn tiền',
          description: 'Đối chiếu giao dịch gốc, số tiền, lý do và chứng từ; người gửi đề nghị không được tự duyệt.',
          action: 'Mở hàng đợi hoàn tiền',
          disabled: !can('REFUND_APPROVE'),
          path: '/admin/payments?tab=refunds',
        },
        {
          title: 'Duyệt phiếu lương',
          description: 'Kiểm tra dữ liệu chấm công, phụ cấp và khấu trừ trước khi cho phép chuyển sang thanh toán.',
          action: 'Mở phiếu chờ duyệt',
          disabled: !can('PAYROLL_APPROVE'),
          path: '/admin/payroll',
        },
        {
          title: 'Khóa lô đối soát ngân hàng',
          description: 'Chỉ khóa lô khi số LoraFilm, nhà cung cấp và ngân hàng đã khớp, không còn dòng ngoại lệ.',
          action: 'Kiểm tra lô đối soát',
          disabled: !can('SETTLEMENT_LOCK'),
          path: '/admin/settlements',
        },
        {
          title: 'Khóa kỳ kế toán',
          description: 'Xác nhận mọi hồ sơ, ca tiền mặt và lô ngân hàng đã hoàn tất trước khi khóa số liệu.',
          action: 'Kiểm tra điều kiện khóa kỳ',
          disabled: !can('ACCOUNTING_PERIOD_CLOSE'),
          path: '/admin/accounting-periods',
        },
        {
          title: 'Rà soát dấu vết kiểm soát',
          description: 'Kiểm tra người thực hiện, thời điểm và căn cứ của các quyết định quan trọng trước khi bàn giao.',
          action: 'Mở nhật ký kiểm soát',
          disabled: !can('AUDIT_VIEW'),
          path: '/admin/accounting-audit',
        },
      ]
    : [
        {
          title: 'Nắm số doanh thu',
          description: 'Xem doanh thu thuần, khoản hoàn và phạm vi dữ liệu trước khi bắt đầu đối soát.',
          action: 'Mở báo cáo',
          disabled: !canViewAnalytics,
          path: '/admin/analytics',
        },
        {
          title: 'Nhập và khớp lô ngân hàng',
          description: 'Nhập sao kê và so ba nguồn: LoraFilm, nhà cung cấp và số ngân hàng thực ghi có.',
          action: 'Mở đối soát ngân hàng',
          disabled: !can('SETTLEMENT_IMPORT'),
          path: '/admin/settlements',
        },
        {
          title: 'Xác minh tiền mặt cuối ca',
          description: 'So tiền hệ thống với tiền nhân viên thực đếm; ca thừa thiếu phải có giải trình.',
          action: 'Mở biên bản chốt ca',
          disabled: !can('CASH_CLOSE_VERIFY'),
          path: '/admin/cash-control',
        },
        {
          title: 'Xử lý chênh lệch và lập đề nghị hoàn',
          description: 'Ghi kết luận hồ sơ lệch, bổ sung căn cứ và chuyển đề nghị hoàn sang người kiểm soát.',
          action: 'Vào hàng đợi xử lý',
          disabled: !canReconcile,
          path: '/admin/payments?tab=reconciliations',
        },
        {
          title: 'Chuẩn bị và gửi bảng lương',
          description: 'Hoàn thiện phiếu đã duyệt, gửi lệnh thanh toán và khớp chứng từ ngân hàng.',
          action: 'Mở quy trình bảng lương',
          disabled: !can('PAYROLL_CREATE', 'PAYROLL_SUBMIT_PAYMENT', 'PAYROLL_RECONCILE'),
          path: '/admin/payroll',
        },
        {
          title: 'Hoàn tất đối soát kỳ',
          description: 'Xác nhận kỳ đã đủ hồ sơ rồi chuyển kế toán kiểm soát thực hiện bước khóa cuối cùng.',
          action: 'Mở kỳ kế toán',
          disabled: !can('ACCOUNTING_PERIOD_RECONCILE'),
          path: '/admin/accounting-periods',
        },
      ];

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
            {isAccountingController ? <ShieldCheck size={17} /> : <BadgeDollarSign size={17} />}
            {workspaceCopy.eyebrow}
          </p>
          <h1 className="mt-2 text-3xl font-black">{workspaceCopy.title}</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">
            {workspaceCopy.description}
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
          {workspaceCopy.responsibility}
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
        {isAccountingController ? <>
          <MetricCard
            icon={CreditCard}
            label="Đề nghị hoàn chờ quyết định"
            value={canViewPayments ? Number(data.refunds?.totalElements) || 0 : '—'}
            hint="Ưu tiên hồ sơ có đủ giao dịch gốc và chứng từ khách hàng"
            tone={Number(data.refunds?.totalElements) ? 'danger' : 'success'}
            onClick={canViewPayments ? () => navigate('/admin/payments?tab=refunds') : undefined}
          />
          <MetricCard
            icon={WalletCards}
            label="Phiếu lương chờ duyệt"
            value={canViewPayroll ? Number(data.payroll?.pendingApproval) || 0 : '—'}
            hint="Không được tự duyệt phiếu do mình lập hoặc phiếu lương của chính mình"
            tone={Number(data.payroll?.pendingApproval) ? 'danger' : 'success'}
            onClick={canViewPayroll ? () => navigate('/admin/payroll') : undefined}
          />
          <MetricCard
            icon={Landmark}
            label="Lô cần rà soát trước khi khóa"
            value={Number(data.accounting?.settlementBatchesNeedReview) || 0}
            hint="Chỉ khóa sau khi ba nguồn đã khớp và không còn ngoại lệ"
            tone={Number(data.accounting?.settlementBatchesNeedReview) ? 'warning' : 'success'}
            onClick={() => navigate('/admin/settlements')}
          />
          <MetricCard
            icon={CalendarClock}
            label="Kỳ đang chờ hoàn tất kiểm soát"
            value={Number(data.accounting?.accountingPeriodsOpen) || 0}
            hint="Kiểm tra toàn bộ điều kiện trước khi khóa số liệu"
            tone={Number(data.accounting?.accountingPeriodsOpen) ? 'warning' : 'neutral'}
            onClick={() => navigate('/admin/accounting-periods')}
          />
        </> : <>
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
            label="Đề nghị hoàn đang chuẩn bị"
            value={canViewPayments ? Number(data.refunds?.totalElements) || 0 : '—'}
            hint="Hoàn thiện căn cứ rồi chuyển kế toán kiểm soát quyết định"
            tone={Number(data.refunds?.totalElements) ? 'warning' : 'neutral'}
            onClick={canViewPayments ? () => navigate('/admin/payments?tab=refunds') : undefined}
          />
        </>}
      </section>

      {canViewAccountingOperations ? <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={Landmark}
          label="Lô ngân hàng cần xử lý"
          value={Number(data.accounting?.settlementBatchesNeedReview) || 0}
          hint="Lô chưa khớp đủ ba nguồn: LoraFilm, nhà cung cấp và ngân hàng"
          tone={Number(data.accounting?.settlementBatchesNeedReview) ? 'danger' : 'success'}
          onClick={() => navigate('/admin/settlements')}
        />
        <MetricCard
          icon={Scale}
          label="Ca tiền mặt chờ xác minh"
          value={Number(data.accounting?.cashSessionsNeedVerification) || 0}
          hint={`Tổng chênh lệch cần kiểm tra: ${money(data.accounting?.cashVarianceNeedReview)}`}
          tone={Number(data.accounting?.cashSessionsNeedVerification) ? 'warning' : 'success'}
          onClick={() => navigate('/admin/cash-control')}
        />
        <MetricCard
          icon={CalendarClock}
          label="Kỳ kế toán đang mở"
          value={Number(data.accounting?.accountingPeriodsOpen) || 0}
          hint="Chỉ khóa khi lô lệch, hồ sơ mở và ca tiền mặt đã được xử lý"
          tone={Number(data.accounting?.accountingPeriodsOpen) ? 'warning' : 'neutral'}
          onClick={() => navigate('/admin/accounting-periods')}
        />
        <MetricCard
          icon={ShieldCheck}
          label="Hồ sơ chặn chốt kỳ"
          value={Number(data.accounting?.reconciliationCasesOpen) || 0}
          hint="Mỗi hồ sơ phải có người nhận xử lý và kết luận có căn cứ"
          tone={Number(data.accounting?.reconciliationCasesOpen) ? 'danger' : 'success'}
          onClick={() => navigate('/admin/payments?tab=reconciliations')}
        />
      </section> : null}

      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.4fr]">
        <div className="space-y-4">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.16em] text-zinc-600">{workspaceCopy.cycleLabel}</p>
            <h2 className="mt-2 text-xl font-black">{workspaceCopy.cycleTitle}</h2>
          </div>
          {workflowSteps.map((step, index) => (
            <WorkflowStep
              key={step.title}
              number={String(index + 1).padStart(2, '0')}
              title={step.title}
              description={step.description}
              action={step.action}
              disabled={step.disabled}
              onClick={() => navigate(step.path)}
            />
          ))}
        </div>

        {isAccountingController ? (
          <section className="overflow-hidden rounded-2xl border border-sky-500/20 bg-sky-500/[0.035]">
            <header className="border-b border-sky-500/15 px-5 py-4">
              <h2 className="flex items-center gap-2 text-sm font-black text-sky-100">
                <ShieldCheck size={17} className="text-sky-400" /> Hàng đợi kiểm soát độc lập
              </h2>
              <p className="mt-1 text-xs leading-5 text-zinc-500">Chọn hồ sơ cần ra quyết định; hệ thống sẽ tiếp tục chặn việc tự duyệt và các hồ sơ chưa đủ điều kiện.</p>
            </header>
            <div className="grid gap-3 p-5 sm:grid-cols-2">
              {[
                {
                  label: 'Đề nghị hoàn tiền',
                  count: Number(data.refunds?.totalElements) || 0,
                  hint: 'Duyệt hoặc từ chối kèm căn cứ',
                  path: '/admin/payments?tab=refunds',
                  allowed: can('REFUND_APPROVE'),
                },
                {
                  label: 'Phiếu lương chờ duyệt',
                  count: Number(data.payroll?.pendingApproval) || 0,
                  hint: 'Kiểm tra trước khi cho thanh toán',
                  path: '/admin/payroll',
                  allowed: can('PAYROLL_APPROVE'),
                },
                {
                  label: 'Lô đối soát cần rà soát',
                  count: Number(data.accounting?.settlementBatchesNeedReview) || 0,
                  hint: 'Khóa khi đủ ba nguồn và hết chênh lệch',
                  path: '/admin/settlements',
                  allowed: can('SETTLEMENT_LOCK'),
                },
                {
                  label: 'Kỳ kế toán đang mở',
                  count: Number(data.accounting?.accountingPeriodsOpen) || 0,
                  hint: 'Khóa sau khi mọi điều kiện đã đạt',
                  path: '/admin/accounting-periods',
                  allowed: can('ACCOUNTING_PERIOD_CLOSE'),
                },
              ].map(item => (
                <button
                  key={item.label}
                  type="button"
                  disabled={!item.allowed}
                  onClick={() => navigate(item.path)}
                  className="flex items-center justify-between gap-4 rounded-2xl border border-white/10 bg-black/20 p-4 text-left transition hover:border-sky-400/35 hover:bg-sky-500/[0.05] disabled:cursor-not-allowed disabled:opacity-35"
                >
                  <span>
                    <strong className="block text-sm text-zinc-100">{item.label}</strong>
                    <span className="mt-1 block text-xs leading-5 text-zinc-500">{item.hint}</span>
                  </span>
                  <span className="grid h-10 min-w-10 place-items-center rounded-xl bg-sky-500/10 px-3 text-lg font-black text-sky-300">{item.count}</span>
                </button>
              ))}
            </div>
          </section>
        ) : (
          <section className="overflow-hidden rounded-2xl border border-zinc-800 bg-zinc-900/60">
          <header className="flex items-center justify-between gap-4 border-b border-zinc-800 px-5 py-4">
            <div>
              <h2 className="flex items-center gap-2 text-sm font-black text-zinc-100">
                <FileSpreadsheet size={17} className="text-orange-400" /> Giao dịch đang cần kiểm tra
              </h2>
              <p className="mt-1 text-xs text-zinc-500">Danh sách ngoại lệ được ưu tiên thay vì bắt người dùng dò toàn bộ giao dịch.</p>
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
              <p className="mt-3 text-sm font-bold text-zinc-300">Không còn giao dịch nào đang chờ kiểm tra</p>
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
        )}
      </section>
    </main>
  );
}
