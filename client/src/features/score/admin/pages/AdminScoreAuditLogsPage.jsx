import { useState, useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { History, Download, RefreshCw, Filter, CheckCircle, AlertCircle } from 'lucide-react';

export default function AdminScoreAuditLogsPage() {
  const {
    auditLogs,
    fetchAuditLogs,
    exportData,
    isLoadingOperations
  } = useAdminScore();

  const [filterUserId, setFilterUserId] = useState('');
  const [filterAction, setFilterAction] = useState('');
  const [notification, setNotification] = useState(null);
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    fetchAuditLogs({ page: 0, size: 30 });
  }, [fetchAuditLogs]);

  const showNotify = (msg, type = 'success') => {
    setNotification({ msg, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleSearch = async (e) => {
    e?.preventDefault();
    const params = { page: 0, size: 30 };
    if (filterUserId.trim()) params.userId = parseInt(filterUserId.trim(), 10);
    if (filterAction.trim()) params.action = filterAction.trim();
    await fetchAuditLogs(params);
  };

  const handleExport = async (type) => {
    setIsExporting(true);
    try {
      const blob = await exportData({ type });
      const url = window.URL.createObjectURL(new Blob([blob]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `loyalty-${type.toLowerCase()}-${Date.now()}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      showNotify(`Đã tải xuống file CSV: ${type}`);
    } catch {
      showNotify('Lỗi khi tải xuống báo cáo CSV', 'error');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-8 p-6 max-w-7xl mx-auto">
      {/* Notification Toast */}
      {notification && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-4 rounded-xl border shadow-2xl backdrop-blur-md transition-all animate-in fade-in slide-in-from-bottom-5 ${
          notification.type === 'error'
            ? 'bg-rose-950/90 border-rose-500/40 text-rose-200'
            : 'bg-emerald-950/90 border-emerald-500/40 text-emerald-200'
        }`}>
          {notification.type === 'error' ? (
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
          ) : (
            <CheckCircle className="w-5 h-5 text-emerald-400 shrink-0" />
          )}
          <span className="text-sm font-bold">{notification.msg}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-zinc-900/40 backdrop-blur-md border border-zinc-800/50 p-6 rounded-[2rem] shadow-2xl shadow-black/20">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <History className="w-7 h-7 text-brand-orange shrink-0" />
            <span>Nhật Ký Kiểm Toán (Audit Logs)</span>
          </h1>
          <p className="text-[11px] font-medium tracking-wide text-zinc-400 mt-1">
            Ghi vết toàn bộ hành động của Admin liên quan đến điều chỉnh điểm, đảo giao dịch và đối soát.
          </p>
        </div>
        <div className="flex flex-wrap gap-2.5">
          <button
            onClick={() => handleExport('AUDIT')}
            disabled={isExporting}
            className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-white/5 hover:bg-white/10 font-black text-[10px] uppercase tracking-widest text-zinc-300 transition-all border border-white/10 shadow-inner disabled:opacity-50 cursor-pointer"
          >
            <Download className="w-4 h-4 text-amber-400" />
            <span>Xuất Audit</span>
          </button>
          <button
            onClick={() => handleExport('HISTORY')}
            disabled={isExporting}
            className="inline-flex items-center justify-center gap-2 px-5 py-3 rounded-2xl bg-white/5 hover:bg-white/10 font-black text-[10px] uppercase tracking-widest text-zinc-300 transition-all border border-white/10 shadow-inner disabled:opacity-50 cursor-pointer"
          >
            <Download className="w-4 h-4 text-emerald-400" />
            <span>Xuất Score History</span>
          </button>
          <button
            onClick={() => handleSearch()}
            disabled={isLoadingOperations}
            className="inline-flex items-center justify-center gap-2 p-3 rounded-2xl bg-white/5 hover:bg-white/10 font-black text-zinc-300 transition-all border border-white/10 shadow-inner disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={`w-4 h-4 ${isLoadingOperations ? 'animate-spin text-brand-orange' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <form onSubmit={handleSearch} className="bg-zinc-900/40 backdrop-blur-md p-6 rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 flex flex-col sm:flex-row gap-5 items-end">
        <div className="flex-1 w-full">
          <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-2">Lọc theo User ID</label>
          <input
            type="number"
            value={filterUserId}
            onChange={(e) => setFilterUserId(e.target.value)}
            placeholder="ID khách hàng..."
            className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-3.5 text-xs text-white font-mono placeholder-zinc-600 focus:outline-none focus:border-brand-orange/50 transition-colors shadow-inner"
          />
        </div>
        <div className="flex-1 w-full">
          <label className="block text-[10px] font-black uppercase tracking-widest text-zinc-500 mb-2">Lọc theo thao tác (Action)</label>
          <select
            value={filterAction}
            onChange={(e) => setFilterAction(e.target.value)}
            className="w-full bg-black/20 border border-zinc-800/80 rounded-2xl px-5 py-3.5 text-xs font-bold text-white focus:outline-none focus:border-brand-orange/50 transition-colors shadow-inner"
          >
            <option value="">Tất cả thao tác</option>
            <option value="ACTION_MANUAL_ADJUSTMENT_ADD">Cộng điểm thủ công (ADD)</option>
            <option value="ACTION_MANUAL_ADJUSTMENT_DEDUCT">Trừ điểm thủ công (DEDUCT)</option>
            <option value="ACTION_MANUAL_ADJUSTMENT_REVERSE">Đảo giao dịch (REVERSE)</option>
            <option value="ACTION_RECONCILIATION_RUN">Kích hoạt đối soát</option>
          </select>
        </div>
        <button
          type="submit"
          disabled={isLoadingOperations}
          className="w-full sm:w-auto px-8 py-3.5 bg-brand-orange hover:bg-opacity-90 text-zinc-950 font-black rounded-2xl text-[11px] uppercase tracking-widest transition-all flex items-center justify-center gap-2 cursor-pointer shadow-xl shadow-brand-orange/20 disabled:opacity-50"
        >
          <Filter className="w-4 h-4" />
          <span>Lọc dữ liệu</span>
        </button>
      </form>

      {/* Audit Table */}
      <div className="bg-zinc-900/40 backdrop-blur-md rounded-[2rem] border border-zinc-800/50 shadow-xl shadow-black/10 overflow-hidden">
        <div className="p-6 border-b border-zinc-800/50 font-black text-sm text-white flex justify-between items-center bg-black/10">
          <span className="tracking-wide">Danh sách nhật ký thao tác Admin</span>
          <span className="text-[10px] font-black uppercase tracking-widest text-zinc-500 bg-black/20 px-3 py-1.5 rounded-xl border border-zinc-800/80 shadow-inner">
            Tổng: {auditLogs?.totalElements || 0} bản ghi
          </span>
        </div>
        <div className="overflow-x-auto p-4">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-800/50 text-[10px] font-black tracking-widest text-zinc-500 uppercase">
                <th className="py-4 px-4">ID</th>
                <th className="py-4 px-4">Thời gian</th>
                <th className="py-4 px-4">Operator</th>
                <th className="py-4 px-4">User ID</th>
                <th className="py-4 px-4">Action</th>
                <th className="py-4 px-4">Method / URI</th>
                <th className="py-4 px-4">IP Address</th>
                <th className="py-4 px-4 text-right">HTTP Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/30 text-xs text-zinc-300">
              {auditLogs && auditLogs.content?.length > 0 ? (
                auditLogs.content.map((log) => (
                  <tr key={log.id} className="hover:bg-white/5 transition-colors group">
                    <td className="py-4 px-4 font-mono text-brand-orange font-bold tracking-wide">#{log.id}</td>
                    <td className="py-4 px-4 font-mono text-zinc-500 tracking-wide group-hover:text-zinc-400 transition-colors">{log.createdAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-4 px-4 font-mono font-black text-white tracking-wide">{log.operatorId || 'ADMIN'}</td>
                    <td className="py-4 px-4 font-mono font-black text-cyan-400 tracking-wide">{log.userId ? `#${log.userId}` : '-'}</td>
                    <td className="py-4 px-4 font-black uppercase text-amber-400 text-[10px] tracking-widest">{log.action}</td>
                    <td className="py-4 px-4 font-mono text-zinc-400 max-w-xs truncate tracking-wide">
                      <span className="font-black text-white mr-2 bg-black/40 px-2 py-1 rounded-md border border-zinc-800/80">{log.httpMethod}</span>
                      {log.requestUri}
                    </td>
                    <td className="py-4 px-4 font-mono text-zinc-500 tracking-wide">{log.clientIp || '127.0.0.1'}</td>
                    <td className="py-4 px-4 text-right">
                      <span className={`inline-flex px-3 py-1 rounded-xl font-mono font-black text-[9px] uppercase tracking-widest shadow-inner ${
                        log.httpStatus === 200 || log.httpStatus === 201 ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
                      }`}>
                        {log.httpStatus || 200}
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8" className="py-16 text-center text-zinc-500 font-black tracking-wide">Không tìm thấy nhật ký kiểm toán nào.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
