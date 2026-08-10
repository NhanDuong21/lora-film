import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Activity, Eye, Info, LockKeyhole, Search, ShieldAlert } from 'lucide-react';
import { getAuthAudits } from '../services/authAdminService';
import { getUserAudits } from '../services/userAdminService';
import { AsyncState, Input, Select } from '@/components/common/ui/uiKit';
import {
  ConsolePagination,
  ConsolePanel,
  DetailDrawer,
  DetailGrid,
  OperationsHeader,
} from '../components/OperationsConsole';
import {
  getAuditActionLabel,
  getAuditTone,
  getDeviceLabel,
  getTargetLabel,
  getTargetTypeLabel,
  summarizeAuditDetails,
} from '../utils/systemPresentation';

const TARGET_TYPES = ['', 'USER', 'CUSTOMER', 'EMPLOYEE', 'PAYROLL', 'DEPARTMENT', 'POSITION', 'ATTENDANCE', 'WORK_SHIFT', 'LEAVE_REQUEST'];

const formatDate = dateString => {
  if (!dateString) return '—';
  return new Date(dateString).toLocaleString('vi-VN');
};

function EventBadge({ action }) {
  const tone = getAuditTone(action);
  const classes = tone === 'danger'
    ? 'border-red-500/25 bg-red-500/10 text-red-300'
    : tone === 'success'
      ? 'border-emerald-500/25 bg-emerald-500/10 text-emerald-300'
      : 'border-sky-500/20 bg-sky-500/10 text-sky-300';
  const label = tone === 'danger' ? 'Cần chú ý' : tone === 'success' ? 'Thành công' : 'Đã ghi nhận';
  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black uppercase tracking-wide ${classes}`}>{label}</span>;
}

export default function AdminAuthAuditPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') === 'operations' ? 'operations' : 'security';
  const [query, setQuery] = useState({ keyword: '', targetType: '', page: 0, size: 20 });
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [state, setState] = useState({ loading: true, error: '' });
  const [selectedEntry, setSelectedEntry] = useState(null);

  const load = useCallback(async () => {
    setState({ loading: true, error: '' });
    try {
      const data = activeTab === 'security'
        ? await getAuthAudits({
            keyword: query.keyword || undefined,
            page: query.page,
            size: query.size,
          })
        : await getUserAudits({
            keyword: query.keyword || undefined,
            targetType: query.targetType || undefined,
            page: query.page,
            size: query.size,
          });
      setResult(data || { content: [], totalPages: 0, totalElements: 0 });
      setState({ loading: false, error: '' });
    } catch (error) {
      setState({ loading: false, error: error?.message || 'Không thể tải nhật ký hoạt động.' });
    }
  }, [activeTab, query]);

  useEffect(() => {
    // Remote audit state is synchronized when the operator changes tab, filter or page.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const changeTab = tab => {
    setSelectedEntry(null);
    setQuery({ keyword: '', targetType: '', page: 0, size: 20 });
    setSearchParams(tab === 'operations' ? { tab: 'operations' } : {});
  };

  return (
    <section className="flex-1 space-y-6 overflow-auto bg-zinc-950 p-6 text-white md:p-8">
      <OperationsHeader
        eyebrow="Hệ thống · Theo dõi và kiểm soát"
        title="Nhật ký hoạt động"
        description="Tra cứu ai đã đăng nhập, thay đổi dữ liệu gì và thời điểm xảy ra. Nội dung kỹ thuật được giữ trong phần chi tiết khi cần đối soát."
      />

      <div className="flex w-fit gap-1 rounded-xl border border-white/10 bg-white/[0.025] p-1">
        <button type="button" onClick={() => changeTab('security')} className={`inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'security' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          <LockKeyhole size={16} /> Đăng nhập & bảo mật
        </button>
        <button type="button" onClick={() => changeTab('operations')} className={`inline-flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-bold transition-colors ${activeTab === 'operations' ? 'bg-brand-orange text-black' : 'text-zinc-400 hover:text-white'}`}>
          <Activity size={16} /> Hoạt động nghiệp vụ
        </button>
      </div>

      <ConsolePanel className="p-4">
        <div className={`grid gap-3 ${activeTab === 'operations' ? 'md:grid-cols-2' : 'md:grid-cols-[minmax(0,1fr)_auto] md:items-center'}`}>
          <div className="relative"><Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-zinc-600" /><Input aria-label="Tìm nhật ký" className="pl-10" placeholder={activeTab === 'security' ? 'Tìm theo hoạt động, địa chỉ IP hoặc mã tài khoản…' : 'Tìm theo hoạt động, đối tượng hoặc mã tài khoản…'} value={query.keyword} onChange={event => setQuery(value => ({ ...value, keyword: event.target.value, page: 0 }))} /></div>
          {activeTab === 'operations' ? (
            <Select aria-label="Lọc phân hệ" value={query.targetType} onChange={event => setQuery(value => ({ ...value, targetType: event.target.value, page: 0 }))}>
              {TARGET_TYPES.map(type => <option key={type || 'ALL'} value={type}>{type ? getTargetTypeLabel(type) : 'Tất cả phân hệ'}</option>)}
            </Select>
          ) : (
            <div className="flex items-center gap-2 px-2 text-xs text-zinc-500"><Info size={15} /><span>Các lần đăng nhập thất bại được đánh dấu để ưu tiên kiểm tra.</span></div>
          )}
        </div>
      </ConsolePanel>

      <AsyncState loading={state.loading} error={state.error} onRetry={load} empty={!result.content?.length} emptyMessage="Không có hoạt động phù hợp" emptyDescription="Hãy thay đổi từ khóa hoặc bộ lọc và thử lại.">
        <ConsolePanel className="overflow-hidden">
          <div className="overflow-x-auto">
            {activeTab === 'security' ? (
              <table className="min-w-full text-left text-sm">
                <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500">
                  <tr><th className="p-4">Thời gian</th><th className="p-4">Hoạt động</th><th className="p-4">Tài khoản</th><th className="p-4">Thiết bị</th><th className="p-4">Kết quả</th><th className="p-4 text-right">Chi tiết</th></tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {result.content?.map(entry => (
                    <tr key={entry.id} className="hover:bg-white/[0.025]">
                      <td className="whitespace-nowrap p-4 text-zinc-400">{formatDate(entry.createdAt)}</td>
                      <td className="p-4"><p className="font-bold text-white">{getAuditActionLabel(entry.action)}</p></td>
                      <td className="p-4"><p className="font-semibold text-zinc-300">{entry.accountId ? `Tài khoản #${entry.accountId}` : 'Hệ thống'}</p></td>
                      <td className="p-4 text-zinc-400">{getDeviceLabel(entry.userAgent)}</td>
                      <td className="p-4"><EventBadge action={entry.action} /></td>
                      <td className="p-4 text-right"><button type="button" onClick={() => setSelectedEntry(entry)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Eye size={14} /> Xem</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <table className="min-w-full text-left text-sm">
                <thead className="bg-white/[0.035] text-[10px] uppercase tracking-wider text-zinc-500">
                  <tr><th className="p-4">Thời gian</th><th className="p-4">Hoạt động</th><th className="p-4">Người thực hiện</th><th className="p-4">Đối tượng</th><th className="p-4">Ghi chú</th><th className="p-4 text-right">Chi tiết</th></tr>
                </thead>
                <tbody className="divide-y divide-white/10">
                  {result.content?.map(entry => (
                    <tr key={entry.id} className="hover:bg-white/[0.025]">
                      <td className="whitespace-nowrap p-4 text-zinc-400">{formatDate(entry.createdAt)}</td>
                      <td className="p-4"><p className="font-bold text-white">{getAuditActionLabel(entry.action)}</p><div className="mt-1"><EventBadge action={entry.action} /></div></td>
                      <td className="p-4 font-semibold text-zinc-300">{entry.actorAccountId ? `Tài khoản #${entry.actorAccountId}` : 'Tác vụ tự động'}</td>
                      <td className="p-4 text-zinc-300">{getTargetLabel(entry.targetType, entry.targetId)}</td>
                      <td className="max-w-sm p-4 text-xs leading-5 text-zinc-500"><span className="line-clamp-2">{summarizeAuditDetails(entry.details)}</span></td>
                      <td className="p-4 text-right"><button type="button" onClick={() => setSelectedEntry(entry)} className="inline-flex items-center gap-1.5 rounded-lg border border-white/10 px-3 py-2 text-xs font-bold text-zinc-300 hover:border-brand-orange/40 hover:text-brand-orange"><Eye size={14} /> Xem</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          <ConsolePagination page={query.page} totalPages={result.totalPages} totalElements={result.totalElements} onPage={page => setQuery(value => ({ ...value, page }))} />
        </ConsolePanel>
      </AsyncState>

      <DetailDrawer open={Boolean(selectedEntry)} onClose={() => setSelectedEntry(null)} title="Chi tiết hoạt động" subtitle={selectedEntry ? getAuditActionLabel(selectedEntry.action) : ''}>
        {selectedEntry ? (
          <div className="space-y-6">
            <div className="flex items-start gap-3 rounded-xl border border-white/10 bg-white/[0.025] p-4">
              <ShieldAlert size={19} className="mt-0.5 shrink-0 text-brand-orange" />
              <div><p className="font-bold text-white">{getAuditActionLabel(selectedEntry.action)}</p><p className="mt-1 text-xs text-zinc-500">{formatDate(selectedEntry.createdAt)}</p></div>
            </div>
            {activeTab === 'security' ? (
              <DetailGrid items={[
                { label: 'Tài khoản', value: selectedEntry.accountId ? `#${selectedEntry.accountId}` : 'Hệ thống' },
                { label: 'Kết quả', value: getAuditTone(selectedEntry.action) === 'danger' ? 'Cần chú ý' : 'Đã ghi nhận' },
                { label: 'Địa chỉ IP', value: selectedEntry.ipAddress || 'Không xác định' },
                { label: 'Thiết bị', value: getDeviceLabel(selectedEntry.userAgent) },
              ]} />
            ) : (
              <DetailGrid items={[
                { label: 'Người thực hiện', value: selectedEntry.actorAccountId ? `Tài khoản #${selectedEntry.actorAccountId}` : 'Tác vụ tự động' },
                { label: 'Đối tượng', value: getTargetLabel(selectedEntry.targetType, selectedEntry.targetId) },
                { label: 'Phân hệ', value: getTargetTypeLabel(selectedEntry.targetType) },
                { label: 'Mã bản ghi', value: `#${selectedEntry.id}` },
              ]} />
            )}
            <div>
              <p className="mb-2 text-[10px] font-black uppercase tracking-[0.18em] text-zinc-600">Ghi chú vận hành</p>
              <div className="rounded-xl border border-white/10 bg-white/[0.025] p-4 text-sm leading-6 text-zinc-300">{activeTab === 'security' ? 'Thông tin thiết bị và địa chỉ mạng được lưu để phục vụ kiểm tra bảo mật.' : summarizeAuditDetails(selectedEntry.details)}</div>
            </div>
            <details className="rounded-xl border border-white/10 p-4">
              <summary className="cursor-pointer text-xs font-bold text-zinc-500">Xem dữ liệu kỹ thuật</summary>
              <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-words text-[11px] leading-5 text-zinc-600">{JSON.stringify(selectedEntry, null, 2)}</pre>
            </details>
          </div>
        ) : null}
      </DetailDrawer>
    </section>
  );
}
