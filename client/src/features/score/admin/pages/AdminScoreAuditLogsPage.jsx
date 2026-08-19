import { useEffect, useMemo, useState } from 'react';
import { useOutletContext, useSearchParams } from 'react-router-dom';
import useAdminScore from '../hooks/useAdminScore';
import { CheckCircle2, Download, History, RefreshCw, X } from 'lucide-react';

const ACTIONS = {
  ACTION_MANUAL_ADJUSTMENT: 'Điều chỉnh điểm có kiểm soát',
  ACTION_MANUAL_ADJUSTMENT_IDEMPOTENT: 'Yêu cầu điều chỉnh trùng đã được chặn',
  ACTION_REVERSE_ADJUSTMENT: 'Hoàn tác điều chỉnh điểm',
  ACTION_SCORE_ACCOUNT_STATUS: 'Thay đổi trạng thái tài khoản điểm',
  ACTION_RECALCULATE_TIER: 'Tính lại hạng thành viên',
  ACTION_RUN_RECONCILIATION: 'Chạy đối soát điểm',
  ACTION_RECONCILIATION_RUN: 'Chạy đối soát điểm',
  ACTION_EXPORT_SCORE_DATA: 'Xuất dữ liệu điểm',
};
const parseJson = value => {
  if (!value) return {};
  try { return typeof value === 'string' ? JSON.parse(value) : value; } catch { return {}; }
};
const when = value => value
  ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value))
  : '—';
const buildParams = filters => ({
  page: 0, size: 50,
  ...(filters.userId ? { userId: Number(filters.userId) } : {}),
  ...(filters.operatorId ? { operatorId: Number(filters.operatorId) } : {}),
  ...(filters.action ? { action: filters.action } : {}),
  ...(filters.from ? { from: `${filters.from}:00` } : {}),
  ...(filters.to ? { to: `${filters.to}:00` } : {}),
});

export default function AdminScoreAuditLogsPage() {
  const outlet = useOutletContext();
  const confirm = outlet?.triggerConfirm || (async () => false);
  const notify = outlet?.triggerToast || (() => undefined);
  const [searchParams] = useSearchParams();
  const { auditLogs, fetchAuditLogs, exportData, isLoadingOperations } = useAdminScore();
  const [filters, setFilters] = useState({ userId: searchParams.get('userId') || '', operatorId: '', action: '', from: '', to: '' });
  const [selectedLog, setSelectedLog] = useState(null);
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    fetchAuditLogs(buildParams({ ...filters, userId: searchParams.get('userId') || filters.userId }));
    // Initial deep link only; later filters are submitted explicitly.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchAuditLogs, searchParams]);

  const rows = useMemo(() => auditLogs?.content || [], [auditLogs]);
  const update = (field, value) => setFilters(current => ({ ...current, [field]: value }));
  const submit = event => { event?.preventDefault(); fetchAuditLogs(buildParams(filters), { forceRefresh: true }); };

  const handleExport = async type => {
    const accepted = await confirm({
      title: 'Xác nhận xuất dữ liệu điểm',
      message: type === 'AUDIT'
        ? 'Tệp chứa nhật ký thao tác quản trị và thông tin kỹ thuật phục vụ điều tra. Chỉ lưu và chia sẻ trong phạm vi được phân quyền.'
        : 'Tệp chứa lịch sử giao dịch điểm của khách hàng trong bộ lọc hiện tại. Chỉ sử dụng đúng mục đích vận hành.',
      confirmLabel: 'Xuất tệp CSV',
    });
    if (!accepted) return;
    setIsExporting(true);
    try {
      const blob = await exportData({
        type, format: 'CSV',
        ...(filters.userId ? { userId: Number(filters.userId) } : {}),
        ...(filters.from ? { from: `${filters.from}:00` } : {}),
        ...(filters.to ? { to: `${filters.to}:00` } : {}),
      });
      const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `du-lieu-diem-${type.toLowerCase()}-${Date.now()}.csv`;
      document.body.appendChild(link); link.click(); link.remove(); window.URL.revokeObjectURL(url);
      notify('Đã xuất tệp CSV và ghi nhận thao tác vào nhật ký.');
      await fetchAuditLogs(buildParams(filters), { forceRefresh: true });
    } catch (error) { notify(error?.response?.data?.message || 'Không thể xuất dữ liệu. Kiểm tra quyền SCORE_EXPORT.', 'error'); }
    finally { setIsExporting(false); }
  };

  return <section className="mx-auto max-w-7xl space-y-6 text-white">
    <header className="flex flex-col gap-4 rounded-3xl border border-white/10 bg-zinc-900/50 p-6 lg:flex-row lg:items-center lg:justify-between"><div><div className="flex items-center gap-2"><History className="text-brand-orange" size={24} /><h1 className="text-2xl font-black">Nhật ký quản trị điểm</h1></div><p className="mt-2 max-w-3xl text-sm leading-6 text-zinc-400">Góc nhìn nghiệp vụ cho bàn giao ca và điều tra: ai thao tác, với khách hàng nào, lý do gì và kết quả ra sao. Chi tiết kỹ thuật nằm trong ngăn riêng.</p></div><div className="flex flex-wrap gap-2"><button type="button" onClick={() => handleExport('AUDIT')} disabled={isExporting} className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-black hover:bg-white/10 disabled:opacity-40"><Download size={15} />Xuất nhật ký</button><button type="button" onClick={() => handleExport('HISTORY')} disabled={isExporting} className="inline-flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-4 py-2.5 text-xs font-black hover:bg-white/10 disabled:opacity-40"><Download size={15} />Xuất lịch sử điểm</button></div></header>

    <form onSubmit={submit} className="grid gap-3 rounded-3xl border border-white/10 bg-zinc-900/40 p-5 md:grid-cols-2 xl:grid-cols-6">
      <FilterField label="Tài khoản khách"><input type="number" value={filters.userId} onChange={event => update('userId', event.target.value)} placeholder="ID tài khoản" className="field" /></FilterField>
      <FilterField label="Người thao tác"><input type="number" value={filters.operatorId} onChange={event => update('operatorId', event.target.value)} placeholder="ID quản trị viên" className="field" /></FilterField>
      <FilterField label="Nghiệp vụ"><select value={filters.action} onChange={event => update('action', event.target.value)} className="field"><option value="">Tất cả nghiệp vụ</option>{Object.entries(ACTIONS).filter(([key]) => key !== 'ACTION_RECONCILIATION_RUN').map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></FilterField>
      <FilterField label="Từ thời điểm"><input type="datetime-local" value={filters.from} onChange={event => update('from', event.target.value)} className="field" /></FilterField>
      <FilterField label="Đến thời điểm"><input type="datetime-local" value={filters.to} onChange={event => update('to', event.target.value)} className="field" /></FilterField>
      <button type="submit" disabled={isLoadingOperations} className="mt-auto inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-brand-orange px-4 text-xs font-black text-black disabled:opacity-40"><RefreshCw size={15} className={isLoadingOperations ? 'animate-spin' : ''} />Áp dụng</button>
    </form>

    <article className="overflow-hidden rounded-3xl border border-white/10 bg-zinc-900/40"><div className="flex items-center justify-between border-b border-white/10 p-5"><h2 className="font-black">Dòng thời gian thao tác</h2><span className="text-xs text-zinc-500">{Number(auditLogs?.totalElements || 0).toLocaleString('vi-VN')} bản ghi</span></div><div className="overflow-x-auto"><table className="min-w-full text-left text-xs"><thead className="bg-white/[0.025] text-[10px] uppercase tracking-wider text-zinc-600"><tr><th className="px-5 py-3">Thời gian</th><th className="px-5 py-3">Người thao tác</th><th className="px-5 py-3">Khách hàng/phạm vi</th><th className="px-5 py-3">Nghiệp vụ</th><th className="px-5 py-3">Lý do / mã vụ việc</th><th className="px-5 py-3">Kết quả</th></tr></thead><tbody className="divide-y divide-white/5">{rows.length ? rows.map(log => <AuditRow key={log.id} log={log} onOpen={() => setSelectedLog(log)} />) : <tr><td colSpan="6" className="p-12 text-center text-zinc-500">Không có nhật ký phù hợp bộ lọc.</td></tr>}</tbody></table></div></article>

    {selectedLog ? <TechnicalDrawer log={selectedLog} onClose={() => setSelectedLog(null)} /> : null}
  </section>;
}

function FilterField({ label, children }) {
  return <label className="text-[10px] font-black uppercase tracking-wider text-zinc-500">{label}<span className="mt-2 block [&_.field]:h-11 [&_.field]:w-full [&_.field]:rounded-xl [&_.field]:border [&_.field]:border-white/10 [&_.field]:bg-black/25 [&_.field]:px-3 [&_.field]:text-xs [&_.field]:font-normal [&_.field]:normal-case [&_.field]:tracking-normal [&_.field]:text-white [&_.field]:outline-none">{children}</span></label>;
}

function AuditRow({ log, onOpen }) {
  const request = parseJson(log.requestPayload);
  const response = parseJson(log.responsePayload);
  const reason = request.reason || response.reason || request.remark || 'Không có lý do nghiệp vụ';
  const caseId = request.caseId || response.caseId;
  const success = !log.httpStatus || Number(log.httpStatus) < 400;
  return <tr onClick={onOpen} className="cursor-pointer hover:bg-white/[0.03]"><td className="whitespace-nowrap px-5 py-4 text-zinc-400">{when(log.createdAt)}</td><td className="px-5 py-4"><p className="font-bold">{log.operatorId ? `Quản trị viên #${log.operatorId}` : 'Tác vụ hệ thống'}</p><p className="mt-1 text-[10px] text-zinc-600">Nhật ký #{log.id}</p></td><td className="px-5 py-4"><p className="font-bold text-cyan-300">{log.userId ? `Tài khoản #${log.userId}` : 'Toàn hệ thống'}</p><p className="mt-1 max-w-44 truncate text-[10px] text-zinc-600">{log.resource || '—'}</p></td><td className="px-5 py-4"><p className="font-bold text-amber-300">{ACTIONS[log.action] || 'Thao tác quản trị điểm'}</p></td><td className="max-w-xs px-5 py-4"><p className="truncate text-zinc-300">{reason}</p><p className="mt-1 text-[10px] text-zinc-600">{caseId ? `Mã vụ việc: ${caseId}` : 'Không gắn mã vụ việc'}</p></td><td className="px-5 py-4"><span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[10px] font-black ${success ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'}`}>{success ? <CheckCircle2 size={12} /> : null}{success ? 'Thành công' : `Lỗi ${log.httpStatus}`}</span><p className="mt-2 text-[10px] text-zinc-600">Mở chi tiết →</p></td></tr>;
}

function TechnicalDrawer({ log, onClose }) {
  const fields = [
    ['Mã thao tác', log.action], ['Phương thức / đường dẫn', `${log.httpMethod || '—'} ${log.requestUri || '—'}`],
    ['Mã phản hồi', log.httpStatus || '—'], ['Địa chỉ mạng', log.clientIp || '—'],
    ['Mã liên kết truy vết', log.correlationId || '—'], ['Mã giao dịch', log.transactionUuid || '—'],
    ['Thiết bị', log.deviceId || '—'], ['Thời lượng', log.durationMs != null ? `${log.durationMs} ms` : '—'],
  ];
  return <div className="fixed inset-0 z-50 flex justify-end bg-black/70" onClick={onClose}><aside role="dialog" aria-modal="true" aria-label="Chi tiết kỹ thuật nhật ký" onClick={event => event.stopPropagation()} className="h-full w-full max-w-2xl overflow-y-auto border-l border-white/10 bg-zinc-950 p-6 shadow-2xl"><div className="flex items-start justify-between"><div><p className="text-[10px] font-black uppercase tracking-wider text-brand-orange">Chi tiết kỹ thuật</p><h2 className="mt-2 text-xl font-black">Nhật ký #{log.id}</h2><p className="mt-1 text-xs text-zinc-500">{when(log.createdAt)}</p></div><button type="button" onClick={onClose} aria-label="Đóng" className="rounded-xl bg-white/5 p-2 text-zinc-400 hover:text-white"><X size={18} /></button></div><dl className="mt-6 grid gap-3 sm:grid-cols-2">{fields.map(([label, value]) => <div key={label} className="rounded-xl bg-white/[0.03] p-3"><dt className="text-[10px] uppercase text-zinc-600">{label}</dt><dd className="mt-1 break-all font-mono text-xs text-zinc-300">{value}</dd></div>)}</dl><Payload title="Dữ liệu yêu cầu" value={log.requestPayload} /><Payload title="Dữ liệu phản hồi" value={log.responsePayload} /><Payload title="Metadata" value={log.metadata} /><div className="mt-5 rounded-xl bg-white/[0.03] p-3"><p className="text-[10px] uppercase text-zinc-600">Trình duyệt / tác nhân</p><p className="mt-2 break-all font-mono text-[10px] leading-5 text-zinc-500">{log.userAgent || '—'}</p></div></aside></div>;
}

function Payload({ title, value }) {
  if (!value) return null;
  let formatted = value;
  try { formatted = JSON.stringify(parseJson(value), null, 2); } catch { /* Keep raw payload. */ }
  return <div className="mt-5"><h3 className="text-xs font-black text-zinc-300">{title}</h3><pre className="mt-2 overflow-x-auto whitespace-pre-wrap break-all rounded-xl border border-white/10 bg-black/30 p-4 text-[10px] leading-5 text-zinc-500">{formatted}</pre></div>;
}
