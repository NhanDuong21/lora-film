import { useCallback, useEffect, useState } from 'react';
import {
  changeEmployeeStatus, getDepartments, getEmployees, getPositions
} from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

export default function AdminStaffPage() {
  const [query, setQuery] = useState({
    keyword: '', status: '', departmentId: '', positionId: '', page: 0, size: 10
  });
  const [options, setOptions] = useState({ departments: [], positions: [] });
  const [result, setResult] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [employees, departments, positions] = await Promise.all([
        getEmployees({
          ...query,
          keyword: query.keyword || undefined,
          status: query.status || undefined,
          departmentId: query.departmentId || undefined,
          positionId: query.positionId || undefined
        }),
        getDepartments(),
        getPositions()
      ]);
      setResult(employees || { content: [], totalPages: 0 });
      setOptions({ departments: departments || [], positions: positions || [] });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhân viên.' });
    }
  }, [query]);
  useEffect(() => { load(); }, [load]);

  const changeStatus = async (employee, action) => {
    try {
      await changeEmployeeStatus(employee.accountId, action);
      await load();
    } catch (error) {
      setState(value => ({ ...value, error: error?.message || 'Không thể đổi trạng thái.' }));
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <div><h1 className="text-2xl font-black uppercase">Quản lý nhân sự</h1>
        <p className="mt-1 text-sm text-zinc-500">Lọc nhân viên theo phòng ban, vị trí và trạng thái làm việc.</p></div>
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <Input placeholder="Tên hoặc mã nhân viên" value={query.keyword}
          onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} />
        <Select value={query.departmentId}
          onChange={event => setQuery(value => ({ ...value, departmentId: event.target.value, page: 0 }))}>
          <option value="">Tất cả phòng ban</option>
          {options.departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </Select>
        <Select value={query.positionId}
          onChange={event => setQuery(value => ({ ...value, positionId: event.target.value, page: 0 }))}>
          <option value="">Tất cả vị trí</option>
          {options.positions.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}
        </Select>
        <Select value={query.status}
          onChange={event => setQuery(value => ({ ...value, status: event.target.value, page: 0 }))}>
          <option value="">Tất cả trạng thái</option><option value="ACTIVE">Hoạt động</option>
          <option value="ON_LEAVE">Nghỉ phép</option><option value="SUSPENDED">Tạm ngưng</option>
          <option value="RESIGNED">Đã nghỉ việc</option>
        </Select>
      </div>
      <AsyncState loading={state.loading} error={state.error} onRetry={load}
        empty={!result.content?.length} emptyMessage="Không có nhân viên">
        <div className="overflow-x-auto rounded-2xl border border-zinc-800">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-zinc-900 text-xs uppercase text-zinc-500"><tr>
              <th className="p-4">Nhân viên</th><th className="p-4">Phòng ban / vị trí</th>
              <th className="p-4">Lương cơ bản</th><th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th></tr></thead>
            <tbody className="divide-y divide-zinc-800">
              {result.content?.map(employee => <tr key={employee.accountId}>
                <td className="p-4"><p className="font-bold">{employee.fullName}</p>
                  <p className="text-xs text-zinc-500">{employee.employeeCode}</p></td>
                <td className="p-4"><p>{employee.departmentName || '—'}</p>
                  <p className="text-xs text-zinc-500">{employee.positionName || '—'}</p></td>
                <td className="p-4">{Number(employee.baseSalary || 0).toLocaleString('vi-VN')} ₫</td>
                <td className="p-4"><StatusBadge status={employee.status} /></td>
                <td className="space-x-3 p-4 text-right">
                  {employee.status === 'SUSPENDED'
                    ? <button onClick={() => changeStatus(employee, 'activate')}
                      className="text-xs font-bold text-emerald-400">Kích hoạt</button>
                    : employee.status !== 'RESIGNED' && <button onClick={() => changeStatus(employee, 'suspend')}
                      className="text-xs font-bold text-amber-400">Tạm ngưng</button>}
                  {employee.status !== 'RESIGNED' && <button onClick={() => changeStatus(employee, 'resign')}
                    className="text-xs font-bold text-red-400">Nghỉ việc</button>}
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
