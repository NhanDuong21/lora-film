import { useCallback, useEffect, useState } from 'react';
import { changePayrollStatus, getPayrolls } from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', maximumFractionDigits: 0
}).format(value || 0);

export default function AdminPayrollPage() {
  const [query, setQuery] = useState({ month: '', status: '', page: 0, size: 10 });
  const [result, setResult] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      setResult(await getPayrolls({
        ...query, month: query.month || undefined, status: query.status || undefined
      }) || { content: [], totalPages: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải bảng lương.' });
    }
  }, [query]);
  useEffect(() => { load(); }, [load]);

  const transition = async (id, action) => {
    try {
      await changePayrollStatus(id, action);
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể cập nhật bảng lương.' }));
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div><h1 className="text-2xl font-black uppercase">Bảng lương nhân viên</h1>
        <p className="mt-1 text-sm text-zinc-500">Duyệt, ghi nhận thanh toán và theo dõi lương theo tháng.</p></div>
      <div className="grid gap-3 md:grid-cols-2">
        <Input type="month" value={query.month}
          onChange={event => setQuery(value => ({ ...value, month: event.target.value, page: 0 }))} />
        <Select value={query.status}
          onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
          <option value="">Tất cả trạng thái</option><option value="PENDING_APPROVAL">Chờ duyệt</option>
          <option value="APPROVED">Đã duyệt</option><option value="PAID">Đã trả</option>
          <option value="CANCELLED">Đã hủy</option>
        </Select>
      </div>
      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={!result.content?.length} emptyMessage="Không có bảng lương">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500"><tr>
              <th className="p-4">Nhân viên</th><th className="p-4">Tháng</th>
              <th className="p-4">Thực nhận</th><th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {result.content?.map(payroll => <tr key={payroll.id}>
                <td className="p-4"><p className="font-bold">{payroll.employeeName}</p>
                  <p className="text-xs text-zinc-500">{payroll.employeeCode}</p></td>
                <td className="p-4">{String(payroll.salaryMonth || '').slice(0, 7)}</td>
                <td className="p-4 font-bold">{money(payroll.totalSalary)}</td>
                <td className="p-4"><StatusBadge status={payroll.status} /></td>
                <td className="space-x-2 p-4 text-right">
                  {payroll.status === 'PENDING_APPROVAL' && <button onClick={() => transition(payroll.id, 'approve')}
                    className="text-xs font-bold text-emerald-400">Duyệt</button>}
                  {payroll.status === 'APPROVED' && <button onClick={() => transition(payroll.id, 'paid')}
                    className="text-xs font-bold text-sky-400">Đã trả</button>}
                  {!['APPROVED', 'PAID', 'CANCELLED'].includes(payroll.status) &&
                    <button onClick={() => transition(payroll.id, 'cancel')}
                      className="text-xs font-bold text-red-400">Hủy</button>}
                </td>
              </tr>)}
            </tbody>
          </table>
        </div>
      </AsyncState>
      <div className="flex justify-end gap-2">
        <button disabled={query.page === 0} onClick={() => setQuery(value => ({ ...value, page: value.page - 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40">Trước</button>
        <button disabled={query.page + 1 >= result.totalPages}
          onClick={() => setQuery(value => ({ ...value, page: value.page + 1 }))}
          className="rounded-lg border border-zinc-800 px-4 py-2 disabled:opacity-40">Sau</button>
      </div>
    </section>
  );
}
