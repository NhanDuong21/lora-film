import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  changeEmployeeStatus, getDepartments, getEmployees, getPositions, createEmployee, updateEmployee
} from '../services/userAdminService';
import { AsyncState, Input, Select, StatusBadge } from '@/components/common/ui/uiKit';

export default function AdminStaffPage() {
  const [query, setQuery] = useState({
    keyword: '', status: '', departmentId: '', positionId: '', page: 0, size: 10
  });
  const [options, setOptions] = useState({ departments: [], positions: [] });
  const [result, setResult] = useState({ content: [], totalPages: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [formData, setFormData] = useState({ fullName: '', employeeCode: '', departmentId: '', positionId: '', baseSalary: '' });

  const openModal = (emp = null) => {
    setEditingEmployee(emp);
    if (emp) {
      setFormData({
        fullName: emp.fullName || '',
        employeeCode: emp.employeeCode || '',
        departmentId: emp.departmentId || '',
        positionId: emp.positionId || '',
        baseSalary: emp.baseSalary || ''
      });
    } else {
      setFormData({ fullName: '', employeeCode: '', departmentId: '', positionId: '', baseSalary: '' });
    }
    setIsModalOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingEmployee) {
        await updateEmployee(editingEmployee.accountId, formData);
      } else {
        await createEmployee(formData);
      }
      setIsModalOpen(false);
      await load();
    } catch (error) {
      alert(error?.message || 'Lỗi khi lưu nhân viên');
    }
  };

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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black uppercase">Quản lý nhân sự</h1>
          <p className="mt-1 text-sm text-zinc-500">Lọc nhân viên theo phòng ban, vị trí và trạng thái làm việc.</p>
        </div>
        <button onClick={() => openModal()} className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">
          + Thêm Nhân viên
        </button>
      </div>
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
                  <Link to={`/admin/staff/${employee.accountId}/documents`} className="text-xs font-bold text-amber-500 hover:underline">Hồ sơ</Link>
                  <button onClick={() => openModal(employee)} className="text-xs font-bold text-blue-400 hover:underline">Sửa</button>
                  {employee.status === 'SUSPENDED'
                    ? <button onClick={() => changeStatus(employee, 'activate')}
                      className="text-xs font-bold text-emerald-400 hover:underline">Kích hoạt</button>
                    : employee.status !== 'RESIGNED' && <button onClick={() => changeStatus(employee, 'suspend')}
                      className="text-xs font-bold text-amber-400 hover:underline">Tạm ngưng</button>}
                  {employee.status !== 'RESIGNED' && <button onClick={() => changeStatus(employee, 'resign')}
                    className="text-xs font-bold text-red-400 hover:underline">Nghỉ việc</button>}
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
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form onSubmit={handleSubmit} className="w-full max-w-lg rounded-2xl border border-zinc-800 bg-zinc-900 p-6 space-y-4">
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-xl font-bold text-white">{editingEmployee ? 'Sửa Nhân viên' : 'Thêm Nhân viên'}</h2>
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Họ và tên</label>
              <Input value={formData.fullName} onChange={e => setFormData({ ...formData, fullName: e.target.value })} required />
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Mã nhân viên</label>
              <Input value={formData.employeeCode} onChange={e => setFormData({ ...formData, employeeCode: e.target.value })} required disabled={!!editingEmployee} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-xs font-bold text-zinc-400">Phòng ban</label>
                <Select value={formData.departmentId} onChange={e => setFormData({ ...formData, departmentId: e.target.value })} required>
                  <option value="">Chọn phòng ban</option>
                  {options.departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                </Select>
              </div>
              <div>
                <label className="text-xs font-bold text-zinc-400">Vị trí</label>
                <Select value={formData.positionId} onChange={e => setFormData({ ...formData, positionId: e.target.value })} required>
                  <option value="">Chọn vị trí</option>
                  {options.positions.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </Select>
              </div>
            </div>
            <div>
              <label className="text-xs font-bold text-zinc-400">Lương cơ bản</label>
              <Input type="number" value={formData.baseSalary} onChange={e => setFormData({ ...formData, baseSalary: e.target.value })} required />
            </div>
            <div className="flex justify-end gap-2 pt-4">
              <button type="button" onClick={() => setIsModalOpen(false)} className="rounded-xl border border-zinc-700 px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800">Hủy</button>
              <button type="submit" className="rounded-xl bg-brand-orange px-4 py-2 text-sm font-bold text-zinc-950 hover:bg-brand-orange/90">Lưu</button>
            </div>
          </form>
        </div>
      )}
    </section>
  );
}
