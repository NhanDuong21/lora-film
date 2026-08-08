import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { BriefcaseBusiness, Building2, ChevronDown, Settings2, UserRound, UsersRound } from 'lucide-react';
import { AsyncState } from '@/components/common/ui/uiKit';
import { getDepartments, getEmployees, getPositions } from '../services/userAdminService';
import useAdminAccess from '../hooks/useAdminAccess';
import { HrHero, PersonAvatar } from '../components/HrWorkspace';

const page = { content: [] };

export default function AdminOrganizationPage() {
  const can = useAdminAccess();
  const [data, setData] = useState({ departments: [], positions: [], employees: [] });
  const [state, setState] = useState({ loading: true, error: '' });
  const [expanded, setExpanded] = useState({});

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const [departments, positions, employees] = await Promise.all([
        can('DEPARTMENT_VIEW') ? getDepartments() : Promise.resolve([]),
        can('POSITION_VIEW') ? getPositions() : Promise.resolve([]),
        can('EMPLOYEE_VIEW') ? getEmployees({ page: 0, size: 200, status: 'ACTIVE' }) : Promise.resolve(page)
      ]);
      setData({ departments: departments || [], positions: positions || [], employees: employees?.content || page.content });
      setExpanded(Object.fromEntries((departments || []).map(item => [item.id, true])));
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải sơ đồ tổ chức.' });
    }
  }, [can]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const departments = useMemo(() => data.departments.map(department => ({
    ...department,
    positions: data.positions.filter(position => position.departmentId === department.id).map(position => ({
      ...position,
      employees: data.employees.filter(employee => employee.positionId === position.id)
    }))
  })), [data]);
  const unassigned = data.employees.filter(employee => !employee.departmentId || !employee.positionId);

  return (
    <section className="min-h-full space-y-5 text-white">
      <HrHero context="Cấu trúc trách nhiệm" title="Sơ đồ tổ chức" description="Nhìn ngay mỗi phòng ban có những vị trí nào, ai đang đảm nhiệm và nơi nào còn trống. Danh mục kỹ thuật được tách khỏi màn hình vận hành hàng ngày." actions={<>{can('DEPARTMENT_UPDATE') ? <Link to="/admin/departments" className="inline-flex items-center gap-2 rounded-xl border border-white/10 px-4 py-2.5 text-sm font-black text-zinc-200 hover:bg-white/5"><Settings2 size={17} /> Chỉnh phòng ban</Link> : null}{can('POSITION_UPDATE') ? <Link to="/admin/positions" className="inline-flex items-center gap-2 rounded-xl bg-orange-500 px-4 py-2.5 text-sm font-black text-black"><BriefcaseBusiness size={17} /> Chỉnh vị trí</Link> : null}</>} />

      <div className="grid gap-3 sm:grid-cols-3">
        {[{ label: 'Phòng ban', value: data.departments.length, icon: Building2, tone: 'text-blue-300 bg-blue-500/10' }, { label: 'Vị trí công việc', value: data.positions.length, icon: BriefcaseBusiness, tone: 'text-purple-300 bg-purple-500/10' }, { label: 'Nhân sự đang làm', value: data.employees.length, icon: UsersRound, tone: 'text-emerald-300 bg-emerald-500/10' }].map(item => { const Icon = item.icon; return <div key={item.label} className="flex items-center justify-between rounded-2xl border border-white/10 bg-[#0b0b0e] p-5"><div><p className="text-xs font-bold text-zinc-500">{item.label}</p><p className="mt-2 text-3xl font-black">{item.value}</p></div><span className={'rounded-xl p-3 ' + item.tone}><Icon size={22} /></span></div>; })}
      </div>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!departments.length} emptyMessage="Chưa có phòng ban trong hệ thống">
        <div className="space-y-4">
          {departments.map(department => (
            <article key={department.id} className="overflow-hidden rounded-[24px] border border-white/10 bg-[#0b0b0e]">
              <button type="button" onClick={() => setExpanded(value => ({ ...value, [department.id]: !value[department.id] }))} className="flex w-full items-center gap-4 border-b border-white/5 p-5 text-left md:p-6">
                <span className="grid h-12 w-12 place-items-center rounded-2xl bg-blue-500/10 text-blue-300"><Building2 size={22} /></span>
                <span className="min-w-0 flex-1"><span className="block text-lg font-black">{department.name}</span><span className="mt-1 block text-xs leading-5 text-zinc-500">{department.description || 'Chưa có mô tả phạm vi trách nhiệm.'}</span></span>
                <span className="hidden gap-6 text-right sm:flex"><span><span className="block text-lg font-black">{department.positions.length}</span><span className="text-[10px] font-bold uppercase text-zinc-600">Vị trí</span></span><span><span className="block text-lg font-black">{department.positions.reduce((sum, position) => sum + position.employees.length, 0)}</span><span className="text-[10px] font-bold uppercase text-zinc-600">Nhân sự</span></span></span>
                <ChevronDown className={'text-zinc-500 transition ' + (expanded[department.id] ? 'rotate-180' : '')} size={20} />
              </button>
              {expanded[department.id] ? (
                <div className="grid gap-3 p-4 md:grid-cols-2 md:p-5 2xl:grid-cols-3">
                  {department.positions.map(position => (
                    <section key={position.id} className="rounded-2xl border border-white/10 bg-black/20 p-4">
                      <div className="flex items-start justify-between gap-3"><div><p className="font-black text-zinc-100">{position.name}</p><p className="mt-1 font-mono text-[10px] text-zinc-600">{position.code}</p></div><span className={'rounded-full px-2.5 py-1 text-[10px] font-black ' + (position.employees.length ? 'bg-emerald-500/10 text-emerald-300' : 'bg-amber-500/10 text-amber-300')}>{position.employees.length ? position.employees.length + ' người' : 'Đang trống'}</span></div>
                      <div className="mt-4 space-y-2">{position.employees.length ? position.employees.map(employee => <Link to="/admin/staff" key={employee.accountId} className="flex items-center gap-3 rounded-xl bg-white/[0.025] p-2.5 hover:bg-white/5"><PersonAvatar name={employee.fullName} /><span className="min-w-0"><span className="block truncate text-xs font-black text-zinc-200">{employee.fullName}</span><span className="mt-0.5 block text-[10px] text-zinc-600">{employee.employeeCode}</span></span></Link>) : <div className="flex items-center gap-2 rounded-xl border border-dashed border-amber-500/20 p-3 text-xs text-amber-200/70"><UserRound size={15} /> Chưa có người đảm nhiệm</div>}</div>
                    </section>
                  ))}
                  {!department.positions.length ? <div className="rounded-2xl border border-dashed border-white/10 p-6 text-center text-sm text-zinc-500">Phòng ban này chưa có vị trí công việc.</div> : null}
                </div>
              ) : null}
            </article>
          ))}
        </div>
      </AsyncState>

      {unassigned.length ? <section className="rounded-2xl border border-amber-500/20 bg-amber-500/[0.04] p-5"><h2 className="font-black text-amber-200">Cần bổ sung cơ cấu</h2><p className="mt-1 text-sm text-amber-200/60">{unassigned.length} nhân viên chưa được gắn đủ phòng ban hoặc vị trí.</p></section> : null}
    </section>
  );
}
