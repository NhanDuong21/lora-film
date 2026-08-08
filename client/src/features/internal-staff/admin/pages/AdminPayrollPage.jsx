import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { CheckCircle2, CircleDollarSign, Clock3, FilePlus2, RefreshCcw, Search, ShieldCheck } from 'lucide-react';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import {
  applyPayrollAction,
  createPayroll,
  generatePayrollFromTimekeeping,
  getEmployees,
  getPayroll,
  getPayrolls,
  getPayrollSummary,
  updatePayroll
} from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
} from '../components/OperationsConsole';
import { HrHero, UatGuide, WorkflowSteps } from '../components/HrWorkspace';

const currentMonth = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};
const emptyPage = { content: [], totalPages: 0, totalElements: 0 };
const emptyPayroll = month => ({ employeeId: '', salaryMonth: month, basicSalary: '', allowance: 0, bonus: 0, deduction: 0 });
const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value || 0);
const STATUS_LABELS = { PENDING_APPROVAL: 'Chờ duyệt', APPROVED: 'Đã duyệt', PAYMENT_PENDING: 'Chờ đối soát', PAID: 'Đã trả', CANCELLED: 'Đã hủy', DRAFT: 'Nháp' };
const ACTION_LABELS = { APPROVE: 'Duyệt phiếu lương', SUBMIT_PAYMENT: 'Gửi lệnh thanh toán', RECONCILE: 'Đối soát ngân hàng/kế toán', CANCEL: 'Hủy phiếu lương' };

export default function AdminPayrollPage() {
  const can = useAdminAccess();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const [query, setQuery] = useState({ month: currentMonth(), status: '', page: 0, size: 15 });
  const [result, setResult] = useState(emptyPage);
  const [summary, setSummary] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [state, setState] = useState({ loading: true, error: '' });
  const [selected, setSelected] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [payrollOpen, setPayrollOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [payrollForm, setPayrollForm] = useState(() => emptyPayroll(currentMonth()));
  const [actionOpen, setActionOpen] = useState(false);
  const [actionForm, setActionForm] = useState({ type: 'APPROVE', reason: '', paymentReference: '', bankBatchReference: '', accountingReference: '', reconciliationMatched: true });
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [page, totals, employeePage] = await Promise.all([
        getPayrolls({ ...query, status: query.status || undefined }),
        getPayrollSummary(query.month),
        can('PAYROLL_CREATE') ? getEmployees({ page: 0, size: 100, status: 'ACTIVE' }) : Promise.resolve(emptyPage)
      ]);
      setResult(page || emptyPage);
      setSummary(totals);
      setEmployees(employeePage?.content || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải kỳ lương.' });
    }
  }, [can, query]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const metrics = useMemo(() => [
    { label: 'Giá trị kỳ lương', value: money(summary?.totalNetAmount), hint: `${summary?.totalRecords || 0} phiếu, không gồm phiếu hủy`, icon: CircleDollarSign, tone: 'orange' },
    { label: 'Chờ kiểm soát', value: summary?.pendingApproval || 0, hint: 'Cần người khác với người tạo duyệt', icon: Clock3, tone: 'amber' },
    { label: 'Duyệt / chờ đối soát', value: `${summary?.approved || 0} / ${summary?.paymentPending || 0}`, hint: 'Lệnh ngân hàng chưa được coi là đã trả', icon: ShieldCheck, tone: 'blue' },
    { label: 'Đã thanh toán', value: summary?.paid || 0, hint: `${summary?.cancelled || 0} phiếu đã hủy`, icon: CheckCircle2, tone: 'green' }
  ], [summary]);

  const workflow = useMemo(() => [
    { label: 'Lấy dữ liệu công', hint: 'Tạo phiếu từ ca làm', state: summary?.totalRecords ? 'done' : 'active' },
    { label: 'Kiểm tra & duyệt', hint: (summary?.pendingApproval || 0) + ' phiếu đang chờ', state: summary?.pendingApproval ? 'active' : summary?.totalRecords ? 'done' : 'waiting' },
    { label: 'Chờ thanh toán', hint: (summary?.approved || 0) + ' phiếu đã duyệt', state: summary?.approved ? 'active' : summary?.paymentPending || summary?.paid ? 'done' : 'waiting' },
    { label: 'Đối soát chứng từ', hint: (summary?.paymentPending || 0) + ' phiếu cần khớp', state: summary?.paymentPending ? 'active' : summary?.paid ? 'done' : 'waiting' },
    { label: 'Hoàn tất', hint: (summary?.paid || 0) + ' phiếu đã trả', state: summary?.paid ? 'done' : 'waiting' }
  ], [summary]);

  const openDetail = async payroll => {
    setSelected(payroll);
    setDetailLoading(true);
    try {
      setSelected(await getPayroll(payroll.id));
    } catch (error) {
      notify(error?.message || 'Không thể tải chi tiết phiếu lương.', 'error');
    } finally {
      setDetailLoading(false);
    }
  };

  const openPayrollForm = (payroll = null) => {
    setEditing(payroll);
    setPayrollForm(payroll ? {
      employeeId: String(payroll.employeeId),
      salaryMonth: String(payroll.salaryMonth).slice(0, 7),
      basicSalary: payroll.basicSalary,
      allowance: payroll.allowance,
      bonus: payroll.bonus,
      deduction: payroll.deduction
    } : emptyPayroll(query.month));
    setPayrollOpen(true);
  };

  const submitPayroll = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        employeeId: Number(payrollForm.employeeId),
        salaryMonth: payrollForm.salaryMonth,
        basicSalary: Number(payrollForm.basicSalary),
        allowance: Number(payrollForm.allowance || 0),
        bonus: Number(payrollForm.bonus || 0),
        deduction: Number(payrollForm.deduction || 0),
        details: []
      };
      if (editing) await updatePayroll(editing.id, payload);
      else await createPayroll(payload);
      setPayrollOpen(false);
      await load();
      notify(editing ? 'Đã cập nhật phiếu lương.' : 'Đã tạo phiếu lương ngoại lệ.');
    } catch (error) {
      notify(error?.message || 'Không thể lưu phiếu lương.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const openAction = type => {
    setActionForm({ type, reason: '', paymentReference: '', bankBatchReference: '', accountingReference: '', reconciliationMatched: true });
    setActionOpen(true);
  };

  const generatePeriod = async () => {
    setSubmitting(true);
    try {
      const outcome = await generatePayrollFromTimekeeping(query.month);
      notify(`Đã sinh ${outcome.generatedCount} phiếu từ chấm công; bỏ qua ${outcome.skippedExisting} phiếu đã có và ${outcome.skippedNoSchedule} nhân viên chưa có ca.`);
      await load();
    } catch (error) {
      notify(error?.message || 'Không thể sinh kỳ lương từ chấm công.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const submitAction = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const updated = await applyPayrollAction(selected.id, {
        ...actionForm,
        paymentReference: actionForm.type === 'RECONCILE' ? actionForm.paymentReference.trim() : null,
        bankBatchReference: actionForm.type === 'SUBMIT_PAYMENT' ? actionForm.bankBatchReference.trim() : null,
        accountingReference: actionForm.type === 'RECONCILE' ? actionForm.accountingReference.trim() : null,
        reconciliationMatched: actionForm.type === 'RECONCILE' ? actionForm.reconciliationMatched : null,
        expectedVersion: selected.version
      });
      setSelected(updated);
      setActionOpen(false);
      await load();
      notify('Đã ghi nhận hành động kỳ lương.');
    } catch (error) {
      notify(error?.message || 'Không thể thực hiện hành động.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const drawerActions = selected ? <div className="grid gap-2 sm:grid-cols-2">
    {can('PAYROLL_UPDATE') && selected.status === 'PENDING_APPROVAL' ? <button type="button" onClick={() => openPayrollForm(selected)} className="rounded-xl border border-white/10 px-4 py-3 text-sm font-black text-zinc-200 hover:bg-white/5">Sửa số liệu</button> : null}
    {can('PAYROLL_APPROVE') && selected.status === 'PENDING_APPROVAL' ? <button type="button" onClick={() => openAction('APPROVE')} className="rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black">Duyệt phiếu</button> : null}
    {can('PAYROLL_UPDATE') && selected.status === 'APPROVED' ? <button type="button" onClick={() => openAction('SUBMIT_PAYMENT')} className="rounded-xl bg-blue-500 px-4 py-3 text-sm font-black text-white">Gửi lệnh thanh toán</button> : null}
    {can('PAYROLL_UPDATE') && selected.status === 'PAYMENT_PENDING' ? <button type="button" onClick={() => openAction('RECONCILE')} className="rounded-xl bg-emerald-500 px-4 py-3 text-sm font-black text-black">Đối soát thanh toán</button> : null}
    {can('PAYROLL_UPDATE') && selected.status === 'PENDING_APPROVAL' ? <button type="button" onClick={() => openAction('CANCEL')} className="rounded-xl border border-red-500/30 px-4 py-3 text-sm font-black text-red-400">Hủy phiếu</button> : null}
  </div> : null;

  return (
    <section className="flex-1 space-y-5 overflow-auto text-white">
      <HrHero context={'Kỳ lương ' + query.month} title="Quy trình bảng lương" description="Làm lần lượt từ dữ liệu chấm công đến đối soát. Mỗi phiếu luôn cho biết đang ở bước nào và nút tiếp theo cần bấm là gì." actions={<><UatGuide compact />{can('PAYROLL_CREATE') ? <><button disabled={submitting} type="button" onClick={generatePeriod} className="flex items-center gap-2 rounded-xl border border-white/10 px-4 py-2.5 text-sm font-black text-white hover:bg-white/5"><RefreshCcw size={18} /> Lấy dữ liệu chấm công</button><button type="button" onClick={() => openPayrollForm()} className="flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black hover:bg-orange-400"><FilePlus2 size={18} /> Tạo phiếu ngoại lệ</button></> : null}</>} />
      <WorkflowSteps steps={workflow} />

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        {metrics.map((item, index) => {
          const Icon = item.icon;
          const statuses = ['', 'PENDING_APPROVAL', 'PAYMENT_PENDING', 'PAID'];
          const selectedStage = query.status === statuses[index];
          return <button key={item.label} type="button" onClick={() => setQuery(value => ({ ...value, status: statuses[index], page: 0 }))} className={'flex items-center justify-between rounded-2xl border p-4 text-left transition ' + (selectedStage ? 'border-orange-500/40 bg-orange-500/[0.06]' : 'border-white/10 bg-[#0b0b0e] hover:bg-white/[0.035]')}><div><p className="text-xs font-bold text-zinc-500">{item.label}</p><p className="mt-2 text-xl font-black text-white">{item.value}</p><p className="mt-1 text-[10px] text-zinc-600">{item.hint}</p></div><span className={'rounded-xl p-3 ' + (index === 0 ? 'bg-orange-500/10 text-orange-300' : index === 1 ? 'bg-amber-500/10 text-amber-300' : index === 2 ? 'bg-blue-500/10 text-blue-300' : 'bg-emerald-500/10 text-emerald-300')}><Icon size={20} /></span></button>;
        })}
      </div>

      <div className="rounded-2xl border border-blue-500/20 bg-blue-500/[0.05] px-5 py-4 text-sm leading-6 text-blue-100/75">
        <span className="font-black text-blue-200">Cách vận hành đúng:</span> lấy dữ liệu chấm công → kiểm tra từng phiếu → tài khoản khác duyệt → nhập mã lô ngân hàng → nhập mã giao dịch và bút toán để đối soát. Chỉ sau bước cuối hệ thống mới ghi nhận “Đã trả”.
      </div>
      <ConsolePanel>
        <div className="grid gap-3 border-b border-white/10 p-4 md:grid-cols-[1fr_260px]"><label className="relative"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} /><input type="month" aria-label="Kỳ lương" value={query.month} onChange={event => setQuery(value => ({ ...value, month: event.target.value, page: 0 }))} className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" /></label><select aria-label="Lọc trạng thái phiếu lương" value={query.status} onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))} className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả trạng thái</option>{Object.entries(STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></div>
        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Kỳ này chưa có phiếu lương">
          <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600"><tr><th className="px-5 py-4">Nhân viên</th><th className="px-5 py-4">Kỳ</th><th className="px-5 py-4">Thực nhận</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4 text-right">Kiểm soát</th></tr></thead><tbody className="divide-y divide-white/5">{result.content?.map(payroll => <tr key={payroll.id} className="hover:bg-white/[0.025]"><td className="px-5 py-4"><p className="font-black text-zinc-100">{payroll.employeeName}</p><p className="mt-1 font-mono text-xs text-zinc-600">{payroll.employeeCode}</p></td><td className="px-5 py-4 font-mono text-zinc-400">{String(payroll.salaryMonth).slice(0, 7)}</td><td className="px-5 py-4 font-black text-zinc-200">{money(payroll.totalSalary)}</td><td className="px-5 py-4"><StatusBadge status={payroll.status} label={STATUS_LABELS[payroll.status]} /></td><td className="px-5 py-4 text-right"><button type="button" onClick={() => openDetail(payroll)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:border-brand-orange/50 hover:text-brand-orange">Mở phiếu</button></td></tr>)}</tbody></table></div>
          <ConsolePagination page={query.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
        </AsyncState>
      </ConsolePanel>

      <DetailDrawer open={Boolean(selected)} onClose={() => setSelected(null)} title={selected?.employeeName || 'Phiếu lương'} subtitle={selected ? `${selected.employeeCode} · ${String(selected.salaryMonth).slice(0, 7)}` : ''} footer={drawerActions}>
        {detailLoading ? <p className="text-sm text-zinc-500">Đang tải phiếu…</p> : selected ? <div className="space-y-6"><DetailGrid items={[{ label: 'Lương cơ bản', value: money(selected.basicSalary) }, { label: 'Phụ cấp', value: money(selected.allowance) }, { label: 'Khấu trừ', value: money(selected.deduction) }, { label: 'Thực nhận', value: money(selected.totalSalary) }, { label: 'Nguồn', value: selected.sourceType === 'TIMEKEEPING' ? 'Ca làm & chấm công' : 'Ngoại lệ thủ công' }, { label: 'Trạng thái', value: STATUS_LABELS[selected.status] }]} />{selected.sourceType === 'TIMEKEEPING' ? <DetailGrid items={[{ label: 'Phút ca', value: selected.scheduledMinutes }, { label: 'Phút công', value: selected.workedMinutes }, { label: 'Nghỉ hưởng lương', value: selected.paidLeaveMinutes }, { label: 'Tăng ca', value: selected.overtimeMinutes }]} /> : null}<div className="rounded-2xl border border-white/10 bg-white/[0.025] p-4 text-sm leading-6 text-zinc-500"><p><span className="font-black text-zinc-300">Người lập:</span> {selected.createdBy ? `Account #${selected.createdBy}` : 'Tác vụ hệ thống'}</p><p><span className="font-black text-zinc-300">Người duyệt:</span> {selected.approvedBy ? `Account #${selected.approvedBy}` : '—'}</p><p><span className="font-black text-zinc-300">Lô ngân hàng:</span> {selected.bankBatchReference || '—'}</p><p><span className="font-black text-zinc-300">Giao dịch:</span> {selected.paymentReference || '—'}</p><p><span className="font-black text-zinc-300">Bút toán:</span> {selected.accountingReference || '—'}</p><p><span className="font-black text-zinc-300">Đối soát:</span> {selected.reconciliationStatus}</p>{selected.cancellationReason ? <p><span className="font-black text-zinc-300">Lý do hủy:</span> {selected.cancellationReason}</p> : null}</div></div> : null}
      </DetailDrawer>

      <ActionModal open={payrollOpen} onClose={() => setPayrollOpen(false)} title={editing ? 'Điều chỉnh phiếu lương' : 'Tạo phiếu lương ngoại lệ'} description="Tác vụ tháng tự sinh phiếu từ lương cơ bản. Form này dành cho trường hợp ngoại lệ và mọi phiếu đều phải qua bước duyệt độc lập." onSubmit={submitPayroll} submitLabel={editing ? 'Lưu số liệu' : 'Tạo phiếu'} submitting={submitting}>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Nhân viên *</label><select required disabled={Boolean(editing)} value={payrollForm.employeeId} onChange={event => { const employee = employees.find(item => String(item.accountId) === event.target.value); setPayrollForm(value => ({ ...value, employeeId: event.target.value, basicSalary: employee?.baseSalary || '' })); }} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange disabled:opacity-50"><option value="">Chọn nhân viên</option>{employees.map(item => <option key={item.accountId} value={item.accountId}>{item.fullName} · {item.employeeCode}</option>)}</select></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Kỳ lương *</label><input required disabled={Boolean(editing)} type="month" value={payrollForm.salaryMonth} onChange={event => setPayrollForm(value => ({ ...value, salaryMonth: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange disabled:opacity-50" /></div>
        <div className="grid gap-3 sm:grid-cols-2">{[['basicSalary', 'Lương cơ bản *'], ['allowance', 'Phụ cấp'], ['bonus', 'Thưởng'], ['deduction', 'Khấu trừ']].map(([key, label]) => <div key={key}><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">{label}</label><input required={key === 'basicSalary'} type="number" min={key === 'basicSalary' ? 1 : 0} value={payrollForm[key]} onChange={event => setPayrollForm(value => ({ ...value, [key]: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div>)}</div>
      </ActionModal>

      <ActionModal open={actionOpen} onClose={() => setActionOpen(false)} title={ACTION_LABELS[actionForm.type]} description={actionForm.type === 'APPROVE' ? 'Người lập phiếu không thể tự duyệt. Hệ thống sẽ kiểm tra ở API.' : 'Hành động này được lưu cùng người thực hiện, thời điểm và version dữ liệu.'} onSubmit={submitAction} submitLabel={ACTION_LABELS[actionForm.type]} submitting={submitting} tone={actionForm.type === 'CANCEL' ? 'danger' : 'orange'}>
        {actionForm.type === 'SUBMIT_PAYMENT' ? <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Mã lô ngân hàng *</label><input required maxLength={100} value={actionForm.bankBatchReference} onChange={event => setActionForm(value => ({ ...value, bankBatchReference: event.target.value }))} placeholder="BANK-BATCH-2026-08-001" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div> : null}
        {actionForm.type === 'RECONCILE' ? <><div className="grid gap-3 sm:grid-cols-2"><label className="text-xs font-black uppercase text-zinc-500">Mã giao dịch ngân hàng *<input required maxLength={100} value={actionForm.paymentReference} onChange={event => setActionForm(value => ({ ...value, paymentReference: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label><label className="text-xs font-black uppercase text-zinc-500">Mã bút toán kế toán *<input required maxLength={100} value={actionForm.accountingReference} onChange={event => setActionForm(value => ({ ...value, accountingReference: event.target.value }))} className="mt-2 w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm" /></label></div><label className="flex items-center gap-3 rounded-xl border border-white/10 p-3 text-sm font-bold"><input type="checkbox" checked={actionForm.reconciliationMatched} onChange={event => setActionForm(value => ({ ...value, reconciliationMatched: event.target.checked }))} /> Chứng từ ngân hàng khớp bút toán kế toán</label></> : null}
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do / căn cứ *</label><textarea required minLength={5} maxLength={500} rows={4} value={actionForm.reason} onChange={event => setActionForm(value => ({ ...value, reason: event.target.value }))} placeholder="Căn cứ duyệt, đối soát hoặc lý do hủy…" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" /></div>
      </ActionModal>
    </section>
  );
}
