import { useCallback, useEffect, useMemo, useState } from 'react';
import { useOutletContext } from 'react-router-dom';
import { BriefcaseBusiness, Building2, Edit2, Plus, Search, Trash2, UserRoundCheck } from 'lucide-react';
import { AsyncState } from '@/components/common/ui/uiKit';
import {
  createPosition,
  deletePosition,
  getDepartments,
  getPositions,
  updatePosition
} from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import { ActionModal, ConsolePanel, MetricStrip, OperationsHeader } from '../components/OperationsConsole';

const emptyForm = () => ({ code: '', name: '', description: '', departmentId: '' });

export default function AdminPositionPage() {
  const can = useAdminAccess();
  const outlet = useOutletContext();
  const notify = outlet?.triggerToast || (() => undefined);
  const confirmAction = outlet?.triggerConfirm || (() => Promise.resolve(true));
  const [positions, setPositions] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [filters, setFilters] = useState({ keyword: '', departmentId: '' });
  const [state, setState] = useState({ loading: true, error: '' });
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [positionData, departmentData] = await Promise.all([getPositions(), getDepartments()]);
      setPositions(positionData || []);
      setDepartments(departmentData || []);
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải danh mục vị trí.' });
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const rows = useMemo(() => positions.filter(position => {
    const keyword = filters.keyword.trim().toLowerCase();
    const matchesText = !keyword || [position.code, position.name, position.description, position.departmentName]
      .some(item => item?.toLowerCase().includes(keyword));
    return matchesText && (!filters.departmentId || String(position.departmentId) === filters.departmentId);
  }), [filters, positions]);

  const metrics = [
    { label: 'Vị trí đang dùng', value: positions.length, hint: 'Mỗi vị trí thuộc đúng một phòng ban', icon: BriefcaseBusiness, tone: 'blue' },
    { label: 'Đã có người đảm nhiệm', value: positions.filter(item => item.activeEmployeeCount > 0).length, hint: `${positions.reduce((sum, item) => sum + item.activeEmployeeCount, 0)} hồ sơ nhân viên`, icon: UserRoundCheck, tone: 'green' },
    { label: 'Phòng ban có vị trí', value: new Set(positions.map(item => item.departmentId)).size, hint: `${departments.length} phòng ban đang hoạt động`, icon: Building2, tone: 'purple' }
  ];

  const openForm = (position = null) => {
    setEditing(position);
    setForm(position ? {
      code: position.code,
      name: position.name,
      description: position.description || '',
      departmentId: String(position.departmentId)
    } : emptyForm());
    setModalOpen(true);
  };

  const submit = async event => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const payload = { ...form, departmentId: Number(form.departmentId) };
      if (editing) await updatePosition(editing.id, payload);
      else await createPosition(payload);
      setModalOpen(false);
      await load();
      notify(editing ? 'Đã cập nhật vị trí.' : 'Đã tạo vị trí.');
    } catch (error) {
      notify(error?.message || 'Không thể lưu vị trí.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async position => {
    if (position.activeEmployeeCount > 0) {
      return notify('Vị trí vẫn đang có nhân viên đảm nhiệm.', 'error');
    }
    if (!await confirmAction(`Lưu trữ vị trí “${position.name}”?`)) return;
    try {
      await deletePosition(position.id);
      await load();
      notify('Đã lưu trữ vị trí.');
    } catch (error) {
      notify(error?.message || 'Không thể lưu trữ vị trí.', 'error');
    }
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-[#050506] p-5 text-white md:p-8">
      <OperationsHeader eyebrow="Job architecture" title="Danh mục vị trí" description="Vị trí được đặt trong phòng ban cụ thể và dùng làm ràng buộc khi tuyển dụng hoặc điều chuyển nhân viên." actions={can('POSITION_CREATE') ? <button type="button" onClick={() => openForm()} className="flex items-center gap-2 rounded-xl bg-brand-orange px-4 py-2.5 text-sm font-black text-black"><Plus size={18} /> Thêm vị trí</button> : null} />
      <MetricStrip items={metrics} />
      <ConsolePanel>
        <div className="grid gap-3 border-b border-white/10 p-4 md:grid-cols-[1fr_260px]"><label className="relative"><Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-600" size={18} /><input aria-label="Tìm vị trí" value={filters.keyword} onChange={event => setFilters(value => ({ ...value, keyword: event.target.value }))} placeholder="Mã, tên, phòng ban hoặc mô tả" className="h-11 w-full rounded-xl border border-white/10 bg-black/30 pl-11 pr-4 text-sm outline-none focus:border-brand-orange" /></label><select aria-label="Lọc phòng ban" value={filters.departmentId} onChange={event => setFilters(value => ({ ...value, departmentId: event.target.value }))} className="h-11 rounded-xl border border-white/10 bg-black/30 px-4 text-sm outline-none focus:border-brand-orange"><option value="">Tất cả phòng ban</option>{departments.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div>
        <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!rows.length} emptyMessage="Không có vị trí phù hợp">
          <div className="overflow-x-auto"><table className="min-w-full text-left text-sm"><thead className="bg-white/[0.025] text-[10px] font-black uppercase tracking-[0.16em] text-zinc-600"><tr><th className="px-5 py-4">Vị trí</th><th className="px-5 py-4">Phòng ban</th><th className="px-5 py-4">Phạm vi công việc</th><th className="px-5 py-4">Nhân sự</th><th className="px-5 py-4 text-right">Quản trị</th></tr></thead><tbody className="divide-y divide-white/5">{rows.map(position => <tr key={position.id} className="hover:bg-white/[0.025]"><td className="px-5 py-4"><p className="font-black text-zinc-100">{position.name}</p><p className="mt-1 font-mono text-xs text-zinc-600">{position.code}</p></td><td className="px-5 py-4"><p className="font-semibold text-zinc-300">{position.departmentName}</p><p className="mt-1 font-mono text-xs text-zinc-600">{position.departmentCode}</p></td><td className="max-w-lg px-5 py-4 text-sm leading-6 text-zinc-500">{position.description || 'Chưa mô tả nhiệm vụ và phạm vi ra quyết định.'}</td><td className="px-5 py-4 font-black text-zinc-300">{position.activeEmployeeCount}</td><td className="px-5 py-4"><div className="flex justify-end gap-2">{can('POSITION_UPDATE') ? <button type="button" aria-label={`Sửa ${position.name}`} onClick={() => openForm(position)} className="rounded-lg border border-white/10 p-2 text-zinc-400 hover:text-white"><Edit2 size={16} /></button> : null}{can('POSITION_DELETE') ? <button type="button" aria-label={`Lưu trữ ${position.name}`} onClick={() => remove(position)} className="rounded-lg border border-white/10 p-2 text-zinc-400 hover:border-red-500/40 hover:text-red-400"><Trash2 size={16} /></button> : null}</div></td></tr>)}</tbody></table></div>
        </AsyncState>
      </ConsolePanel>
      <ActionModal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Cập nhật vị trí' : 'Tạo vị trí'} description="Vị trí bắt buộc thuộc một phòng ban. Nhân viên chỉ có thể được phân vào vị trí cùng phòng ban." onSubmit={submit} submitLabel={editing ? 'Lưu thay đổi' : 'Tạo vị trí'} submitting={submitting}>
        <div className="grid gap-3 sm:grid-cols-[160px_1fr]"><div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Mã *</label><input required pattern="[A-Za-z0-9_]{2,30}" value={form.code} onChange={event => setForm(value => ({ ...value, code: event.target.value.toUpperCase() }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 font-mono text-sm outline-none focus:border-brand-orange" /></div><div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Tên vị trí *</label><input required maxLength={100} value={form.name} onChange={event => setForm(value => ({ ...value, name: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange" /></div></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Phòng ban *</label><select required value={form.departmentId} onChange={event => setForm(value => ({ ...value, departmentId: event.target.value }))} className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm outline-none focus:border-brand-orange"><option value="">Chọn phòng ban quản lý vị trí</option>{departments.map(item => <option key={item.id} value={item.id}>{item.name} · {item.code}</option>)}</select></div>
        <div><label className="mb-2 block text-xs font-black uppercase tracking-wider text-zinc-500">Nhiệm vụ và phạm vi</label><textarea rows={4} maxLength={255} value={form.description} onChange={event => setForm(value => ({ ...value, description: event.target.value }))} placeholder="Nhiệm vụ chính, phạm vi quyết định và kết quả đầu ra…" className="w-full rounded-xl border border-white/10 bg-black/30 p-3 text-sm leading-6 outline-none focus:border-brand-orange" /></div>
      </ActionModal>
    </section>
  );
}
