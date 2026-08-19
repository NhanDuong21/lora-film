import { useCallback, useDeferredValue, useEffect, useMemo, useState } from 'react';
import { useNavigate, useOutletContext } from 'react-router-dom';
import {
  applyCustomerAccessAction,
  getCustomer,
  getCustomers,
  getDashboard
} from '../services/userAdminService';
import { AsyncState, StatusBadge } from '@/components/common/ui/uiKit';
import useAdminAccess from '../hooks/useAdminAccess';
import {
  ActionModal,
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  MetricStrip,
  OperationsHeader
} from '../components/OperationsConsole';
import { Download, Search, ShieldAlert, UserCheck, UserPlus, Users, WalletCards } from 'lucide-react';

const EMPTY_RESULT = { content: [], totalPages: 0, totalElements: 0 };

export default function AdminMembersPage() {
  const can = useAdminAccess();
  const canUpdate = can('CUSTOMER_UPDATE');
  const navigate = useNavigate();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirm = outlet?.triggerConfirm || (async () => false);
  const [filters, setFilters] = useState({ keyword: '', status: '', page: 0, size: 15 });
  const deferredKeyword = useDeferredValue(filters.keyword);
  const [result, setResult] = useState(EMPTY_RESULT);
  const [dashboard, setDashboard] = useState(null);
  const [state, setState] = useState({ loading: true, error: '' });
  const [selected, setSelected] = useState(null);
  const [selectedLoading, setSelectedLoading] = useState(false);
  const [accessAction, setAccessAction] = useState(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const query = useMemo(() => ({
    page: filters.page,
    size: filters.size,
    keyword: deferredKeyword.trim() || undefined,
    status: filters.status || undefined
  }), [deferredKeyword, filters.page, filters.size, filters.status]);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [customers, summary] = await Promise.all([
        getCustomers(query),
        can('DASHBOARD_VIEW') ? getDashboard() : Promise.resolve(null)
      ]);
      setResult(customers || EMPTY_RESULT);
      setDashboard(summary);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh sách khách hàng.' });
    }
  }, [can, query]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const openCustomer = async customer => {
    setSelected(customer);
    setSelectedLoading(true);
    try {
      setSelected(await getCustomer(customer.id));
    } catch (error) {
      notify(error?.message || 'Không thể tải hồ sơ khách hàng.', 'error');
    } finally {
      setSelectedLoading(false);
    }
  };

  const submitAccessAction = async event => {
    event.preventDefault();
    if (!accessAction || reason.trim().length < 5) return;
    setSubmitting(true);
    try {
      const updated = await applyCustomerAccessAction(accessAction.customer.id, {
        type: accessAction.type,
        reason: reason.trim()
      });
      setSelected(updated);
      setAccessAction(null);
      setReason('');
      await load();
      notify(accessAction.type === 'BLOCK' ? 'Đã khóa quyền truy cập khách hàng.' : 'Đã khôi phục quyền truy cập.');
    } catch (error) {
      notify(error?.message || 'Không thể cập nhật quyền truy cập.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const exportVisible = async () => {
    const accepted = await confirm({
      title: 'Xác nhận xuất danh sách khách hàng',
      message: `Tệp chứa email và số điện thoại của ${result.content.length} khách hàng đang hiển thị. Chỉ lưu và chia sẻ trong phạm vi được phân quyền.`,
      confirmLabel: 'Xuất tệp CSV',
    });
    if (!accepted) return;
    const safeCell = value => {
      let text = String(value ?? '');
      if (/^[=+\-@\t\r]/.test(text)) text = `'${text}`;
      return `"${text.replaceAll('"', '""')}"`;
    };
    const rows = [
      ['Mã khách hàng', 'Họ tên', 'Email', 'Số điện thoại', 'Trạng thái', 'Ngày tham gia'],
      ...result.content.map(item => [item.customerCode, item.fullName, item.email, item.phoneNumber, item.status, item.joinedAt])
    ];
    const csv = rows.map(row => row.map(safeCell).join(',')).join('\n');
    const url = URL.createObjectURL(new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = `customers-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    notify('Đã xuất danh sách đang hiển thị.');
  };

  const metrics = [
    { label: 'Khách hàng', value: dashboard?.totalCustomers ?? result.totalElements, hint: 'Không gồm tài khoản nhân sự', icon: Users, tone: 'border-blue-500/20 bg-blue-500/10 text-blue-400' },
    { label: 'Đang hoạt động', value: dashboard?.activeCustomers ?? '—', hint: 'Có thể đăng nhập và đặt vé', icon: UserCheck, tone: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400' },
    { label: 'Bị khóa', value: dashboard?.blockedCustomers ?? '—', hint: 'Yêu cầu xử lý có lý do', icon: ShieldAlert, tone: 'border-red-500/20 bg-red-500/10 text-red-400' },
    { label: 'Kết quả hiện tại', value: result.totalElements, hint: deferredKeyword || filters.status ? 'Theo bộ lọc' : 'Toàn bộ hồ sơ', icon: UserPlus, tone: 'border-orange-500/20 bg-orange-500/10 text-orange-400' }
  ];

  return (
    <section className="min-h-full space-y-6 bg-[#050506] p-5 text-white md:p-8">
      <OperationsHeader
        eyebrow="Vận hành khách hàng"
        title="Trung tâm khách hàng"
        description="Tra cứu hồ sơ thành viên, kiểm soát quyền truy cập và theo dõi trạng thái bằng dữ liệu vận hành thực. Tài khoản nhân sự được loại khỏi không gian này."
        actions={(
          <button type="button" onClick={exportVisible} disabled={!result.content.length} className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-sm font-black text-zinc-200 hover:bg-white/10 disabled:opacity-40">
            <Download size={17} /> Xuất kết quả đang xem
          </button>
        )}
      />

      <MetricStrip items={metrics} />

      <ConsolePanel>
        <div className="flex flex-col gap-3 border-b border-white/10 p-4 lg:flex-row">
          <label className="relative flex-1">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} />
            <input
              value={filters.keyword}
              onChange={event => setFilters(value => ({ ...value, keyword: event.target.value, page: 0 }))}
              placeholder="Tên, email, số điện thoại hoặc mã khách hàng"
              aria-label="Tìm khách hàng"
              className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none transition focus:border-brand-orange"
            />
          </label>
          <select
            value={filters.status}
            onChange={event => setFilters(value => ({ ...value, status: event.target.value, page: 0 }))}
            aria-label="Lọc trạng thái khách hàng"
            className="h-11 min-w-56 rounded-xl border border-white/10 bg-black/30 px-4 text-sm font-semibold outline-none focus:border-brand-orange"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Đang hoạt động</option>
            <option value="BLOCKED">Bị khóa</option>
            <option value="INACTIVE">Ngừng hoạt động</option>
          </select>
        </div>

        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content.length} emptyMessage="Không có khách hàng phù hợp">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600">
                <tr><th className="px-5 py-4">Khách hàng</th><th className="px-5 py-4">Liên hệ</th><th className="px-5 py-4">Tham gia</th><th className="px-5 py-4">Trạng thái</th><th className="px-5 py-4 text-right">Hồ sơ</th></tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {result.content.map(customer => (
                  <tr key={customer.id} className="group hover:bg-white/[0.025]">
                    <td className="px-5 py-4"><p className="font-black text-zinc-100">{customer.fullName}</p><p className="mt-1 font-mono text-xs text-zinc-600">{customer.customerCode}</p></td>
                    <td className="px-5 py-4"><p className="text-zinc-300">{customer.email || '—'}</p><p className="mt-1 text-xs text-zinc-600">{customer.phoneNumber || 'Chưa cập nhật số điện thoại'}</p></td>
                    <td className="px-5 py-4 text-zinc-400">{customer.joinedAt || '—'}</td>
                    <td className="px-5 py-4"><StatusBadge status={customer.status} label={customer.status === 'ACTIVE' ? 'Hoạt động' : customer.status === 'BLOCKED' ? 'Bị khóa' : 'Ngừng hoạt động'} /></td>
                    <td className="px-5 py-4 text-right"><button type="button" onClick={() => openCustomer(customer)} className="rounded-lg border border-white/10 px-3 py-2 text-xs font-black text-zinc-300 hover:border-brand-orange/50 hover:text-brand-orange">Mở hồ sơ</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <ConsolePagination page={filters.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setFilters(value => ({ ...value, page }))} />
        </AsyncState>
      </ConsolePanel>

      <DetailDrawer
        open={Boolean(selected)}
        onClose={() => setSelected(null)}
        title={selected?.fullName || 'Hồ sơ khách hàng'}
        subtitle={selected?.customerCode}
        footer={selected ? (
          <div className="grid gap-2">
            {selected.accountId ? (
              <button
                type="button"
                onClick={() => navigate(`/admin/scores/viewer?accountId=${selected.accountId}&customerCode=${encodeURIComponent(selected.customerCode || '')}&name=${encodeURIComponent(selected.fullName || '')}`)}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 py-3 text-sm font-black text-black hover:bg-orange-400"
              >
                <WalletCards size={17} /> Mở hồ sơ điểm thưởng
              </button>
            ) : null}
            {canUpdate ? (
              <button
                type="button"
                onClick={() => { setReason(''); setAccessAction({ customer: selected, type: selected.status === 'BLOCKED' ? 'UNBLOCK' : 'BLOCK' }); }}
                className={`w-full rounded-xl px-4 py-3 text-sm font-black ${selected.status === 'BLOCKED' ? 'bg-emerald-500 text-black hover:bg-emerald-400' : 'bg-red-500/10 text-red-400 ring-1 ring-inset ring-red-500/30 hover:bg-red-500/20'}`}
              >
                {selected.status === 'BLOCKED' ? 'Khôi phục quyền đăng nhập' : 'Khóa quyền đăng nhập'}
              </button>
            ) : null}
          </div>
        ) : null}
      >
        {selectedLoading ? <p className="text-sm text-zinc-500">Đang tải hồ sơ…</p> : selected ? (
          <div className="space-y-6">
            <div className="rounded-2xl border border-white/10 bg-gradient-to-br from-orange-500/10 to-transparent p-5">
              <p className="text-[10px] font-black uppercase tracking-[0.18em] text-zinc-500">Trạng thái tài khoản</p>
              <div className="mt-3"><StatusBadge status={selected.status} label={selected.status === 'ACTIVE' ? 'Đang hoạt động' : selected.status === 'BLOCKED' ? 'Đã khóa' : 'Ngừng hoạt động'} /></div>
              <p className="mt-3 text-xs leading-5 text-zinc-500">Mọi thay đổi quyền truy cập đều yêu cầu lý do và được ghi vào audit log.</p>
            </div>
            <DetailGrid items={[
              { label: 'Email', value: selected.email },
              { label: 'Số điện thoại', value: selected.phoneNumber },
              { label: 'Giới tính', value: selected.gender },
              { label: 'Ngày sinh', value: selected.birthday },
              { label: 'Ngày tham gia', value: selected.joinedAt },
              { label: 'Account ID', value: selected.accountId }
            ]} />
            <div>
              <h3 className="text-sm font-black text-white">Ghi chú vận hành</h3>
              <p className="mt-2 rounded-xl border border-white/10 bg-white/[0.025] p-4 text-sm leading-6 text-zinc-400">{selected.note || 'Chưa có ghi chú cho khách hàng này.'}</p>
            </div>
          </div>
        ) : null}
      </DetailDrawer>

      <ActionModal
        open={Boolean(accessAction)}
        onClose={() => setAccessAction(null)}
        title={accessAction?.type === 'BLOCK' ? 'Khóa quyền truy cập' : 'Khôi phục quyền truy cập'}
        description={accessAction?.type === 'BLOCK' ? 'Tài khoản sẽ không thể đăng nhập. Hãy ghi lý do đủ rõ để ca trực sau có thể xử lý.' : 'Khách hàng sẽ có thể đăng nhập lại. Lý do khôi phục được lưu vào audit log.'}
        onSubmit={submitAccessAction}
        submitLabel={accessAction?.type === 'BLOCK' ? 'Xác nhận khóa' : 'Xác nhận khôi phục'}
        submitting={submitting}
        tone={accessAction?.type === 'BLOCK' ? 'danger' : 'orange'}
      >
        <label className="block text-xs font-black uppercase tracking-wider text-zinc-500">Lý do *</label>
        <textarea value={reason} onChange={event => setReason(event.target.value)} minLength={5} maxLength={500} required rows={4} placeholder="Mô tả nguyên nhân và căn cứ xử lý…" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" />
      </ActionModal>
    </section>
  );
}
