import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { Building2, Edit2, Plus, Search, Trash2, UserRoundCheck, UsersRound } from 'lucide-react';
import { AsyncState } from '@/components/common/ui/uiKit';
import {
  createDepartment,
  deleteDepartment,
  getDepartments,
  getPositions,
  updateDepartment
} from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import { ActionModal, ConsolePanel, MetricStrip, OperationsHeader } from '../components/OperationsConsole';

const emptyForm = () => ({ code: '', name: '', description: '' });

export default function AdminDepartmentPage() {
  const can = useAdminAccess();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const [departments, setDepartments] = useState([]);
  const [positions, setPositions] = useState([]);
  const [keyword, setKeyword] = useState('');
  const [state, setState] = useState({ loading: true, error: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [departmentData, positionData] = await Promise.all([getDepartments(), getPositions()]);
      setDepartments(departmentData || []);
      setPositions(positionData || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải cơ cấu tổ chức.' });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const rows = useMemo(() => departments.filter(department => {
    const value = keyword.trim().toLowerCase();
    return !value || [department.code, department.name, department.description]
      .some(item => item?.toLowerCase().includes(value));
  }), [departments, keyword]);

  const metrics = [
    { label: 'Đơn vị đang dùng', value: departments.length, hint: 'Phòng ban chưa bị lưu trữ', icon: Building2, tone: 'blue' },
    { label: 'Có nhân sự', value: departments.filter(item => item.activeEmployeeCount > 0).length, hint: `${departments.reduce((sum, item) => sum + item.activeEmployeeCount, 0)} hồ sơ được phân bổ`, icon: UserRoundCheck, tone: 'green' },
    { label: 'Chưa có nhân sự', value: departments.filter(item => item.activeEmployeeCount === 0).length, hint: 'Cần rà soát trước khi vận hành', icon: UsersRound, tone: 'amber' }
  ];

  const openForm = (department = null) => {
    setEditing(department);
    setForm(department ? {
      code: department.code,
      name: department.name,
      description: department.description || ''
    } : emptyForm());
    setModalOpen(true);
  };

  const submit = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      if (editing) await updateDepartment(editing.id, form);
      else await createDepartment(form);
      setModalOpen(false);
      await load();
      notify(editing ? 'Đã cập nhật phòng ban.' : 'Đã tạo phòng ban.');
    } catch (error) {
      notify(error?.message || 'Không thể lưu phòng ban.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async department => {
    const usage = department.activePositionCount > 0 || positions.some(item => item.departmentId === department.id)
      ? 'Phòng ban vẫn còn vị trí nên chưa thể lưu trữ.'
      : department.activeEmployeeCount > 0
        ? 'Phòng ban vẫn còn nhân viên nên chưa thể lưu trữ.' : '';
    if (usage) return notify(usage, 'error');
    if (!await confirmAction(`Lưu trữ phòng ban “${department.name}”?`)) return;
    try {
      await deleteDepartment(department.id);
      await load();
      notify('Đã lưu trữ phòng ban.');
    } catch (error) {
      notify(error?.message || 'Không thể lưu trữ phòng ban.', 'error');
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-5 text-white md:p-8">
      <OperationsHeader eyebrow="Organization design" title="Cơ cấu phòng ban" description="Quản trị đơn vị tổ chức cùng số vị trí và nhân sự thực tế. Các chỉ số dưới đây được tính từ dữ liệu, không dùng KPI minh họa." actions={can('DEPARTMENT_CREATE') ? <button type="button" onClick={() => openForm()} className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black"><Plus size={18} /> Thêm phòng ban</button> : null} />
      <MetricStrip items={metrics} />
      <ConsolePanel>
        <div className="border-b border-white/10 p-4">
          <label className="relative block"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} /><input aria-label="Tìm phòng ban" value={keyword} onChange={event => setKeyword(event.target.value)} placeholder="Mã, tên hoặc chức năng phòng ban" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" /></label>
        </div>
        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!rows.length} emptyMessage="Không có phòng ban phù hợp">
          <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600"><tr><th className="px-5 py-4">Đơn vị</th><th className="px-5 py-4">Chức năng</th><th className="px-5 py-4">Vị trí</th><th className="px-5 py-4">Nhân sự</th><th className="px-5 py-4 text-right">Quản trị</th></tr></thead><tbody className="divide-y divide-white/5">{rows.map(department => <tr key={department.id} className="hover:bg-white/[0.025]"><td className="px-5 py-4"><p className="font-black text-zinc-100">{department.name}</p><p className="mt-1 font-mono text-xs text-zinc-600">{department.code}</p></td><td className="max-w-lg px-5 py-4 text-sm leading-6 text-zinc-500">{department.description || 'Chưa mô tả chức năng và phạm vi trách nhiệm.'}</td><td className="px-5 py-4 font-black text-zinc-300">{department.activePositionCount}</td><td className="px-5 py-4 font-black text-zinc-300">{department.activeEmployeeCount}</td><td className="px-5 py-4"><div className="flex justify-end gap-2">{can('DEPARTMENT_UPDATE') ? <button type="button" aria-label={`Sửa ${department.name}`} onClick={() => openForm(department)} className="rounded-lg border border-white/10 p-2 text-zinc-400 hover:text-white"><Edit2 size={16} /></button> : null}{can('DEPARTMENT_DELETE') ? <button type="button" aria-label={`Lưu trữ ${department.name}`} onClick={() => remove(department)} className="rounded-lg border border-white/10 p-2 text-zinc-400 hover:border-red-500/40 hover:text-red-400"><Trash2 size={16} /></button> : null}</div></td></tr>)}</tbody></table></div>
        </AsyncState>
      </ConsolePanel>
      <ActionModal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Cập nhật phòng ban' : 'Tạo phòng ban'} description="Mã là định danh tích hợp; tên và mô tả cần phản ánh đúng phạm vi trách nhiệm." onSubmit={submit} submitLabel={editing ? 'Lưu thay đổi' : 'Tạo phòng ban'} submitting={submitting}>
        <div className="grid gap-3 sm:grid-cols-[140px_1fr]"><div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Mã *</label><input required pattern="[A-Za-z0-9_]{2,20}" value={form.code} onChange={event => setForm(value => ({ ...value, code: event.target.value.toUpperCase() }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 font-mono text-sm outline-none focus:border-brand-orange" /></div><div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Tên phòng ban *</label><input required maxLength={100} value={form.name} onChange={event => setForm(value => ({ ...value, name: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Chức năng và phạm vi</label><textarea rows={4} maxLength={255} value={form.description} onChange={event => setForm(value => ({ ...value, description: event.target.value }))} placeholder="Ví dụ: Vận hành rạp, điều phối suất chiếu và chất lượng dịch vụ…" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" /></div>
      </ActionModal>
    </section>
  );
}
