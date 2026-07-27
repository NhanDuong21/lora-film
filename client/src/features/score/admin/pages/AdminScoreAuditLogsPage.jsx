import { useState, useEffect } from 'react';
import useAdminScore from '../hooks/useAdminScore';
import { History, Download, RefreshCw, Search, Shield, Filter, CheckCircle, AlertCircle } from 'lucide-react';

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
    } catch (err) {
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-zinc-900/60 p-6 rounded-2xl border border-zinc-800/80 backdrop-blur-md">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
            <History className="w-7 h-7 text-amber-500 shrink-0" />
            <span>Nhật Ký Kiểm Toán (Audit Logs & Reports)</span>
          </h1>
          <p className="text-sm text-zinc-400 mt-1">
            Ghi vết toàn bộ hành động của Admin liên quan đến điều chỉnh điểm, đảo giao dịch và đối soát.
          </p>
        </div>
        <div className="flex flex-wrap gap-2.5">
          <button
            onClick={() => handleExport('AUDIT')}
            disabled={isExporting}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-amber-500 hover:bg-amber-600 font-bold text-xs text-zinc-950 transition-all shadow-md shadow-amber-500/10 disabled:opacity-50 cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Xuất CSV Audit</span>
          </button>
          <button
            onClick={() => handleExport('HISTORY')}
            disabled={isExporting}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-emerald-500 hover:bg-emerald-600 font-bold text-xs text-zinc-950 transition-all shadow-md shadow-emerald-500/10 disabled:opacity-50 cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Xuất CSV Score History</span>
          </button>
          <button
            onClick={() => handleSearch()}
            disabled={isLoadingOperations}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-zinc-800 hover:bg-zinc-700 font-bold text-xs text-white transition-all border border-zinc-700/60 shadow-sm disabled:opacity-50 cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoadingOperations ? 'animate-spin text-amber-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <form onSubmit={handleSearch} className="bg-zinc-900/80 p-6 rounded-2xl border border-zinc-800 flex flex-col sm:flex-row gap-4 items-end">
        <div className="flex-1 w-full">
          <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">Lọc theo User ID</label>
          <input
            type="number"
            value={filterUserId}
            onChange={(e) => setFilterUserId(e.target.value)}
            placeholder="ID khách hàng..."
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white font-mono"
          />
        </div>
        <div className="flex-1 w-full">
          <label className="block text-xs font-bold uppercase tracking-wider text-zinc-400 mb-2">Lọc theo mã thao tác (Action)</label>
          <select
            value={filterAction}
            onChange={(e) => setFilterAction(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-amber-500"
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
          className="w-full sm:w-auto px-6 py-2.5 bg-zinc-800 hover:bg-zinc-700 text-white font-bold rounded-xl text-sm transition-all flex items-center justify-center gap-2 cursor-pointer border border-zinc-700/60"
        >
          <Filter className="w-4 h-4 text-amber-400" />
          <span>Lọc dữ liệu</span>
        </button>
      </form>

      {/* Audit Table */}
      <div className="bg-zinc-900/80 rounded-2xl border border-zinc-800 overflow-hidden">
        <div className="p-5 border-b border-zinc-800 font-bold text-sm text-white flex justify-between items-center">
          <span>Danh sách nhật ký thao tác Admin</span>
          <span className="text-xs font-mono text-zinc-400">Tổng: {auditLogs?.totalElements || 0} bản ghi</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-zinc-800/80 text-[11px] font-bold text-zinc-400 uppercase bg-zinc-950/40">
                <th className="py-3 px-4">ID</th>
                <th className="py-3 px-4">Thời gian</th>
                <th className="py-3 px-4">Operator</th>
                <th className="py-3 px-4">User ID</th>
                <th className="py-3 px-4">Action</th>
                <th className="py-3 px-4">Method / URI</th>
                <th className="py-3 px-4">IP Address</th>
                <th className="py-3 px-4 text-right">HTTP Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/60 text-xs text-zinc-300">
              {auditLogs && auditLogs.content?.length > 0 ? (
                auditLogs.content.map((log) => (
                  <tr key={log.id} className="hover:bg-zinc-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono text-amber-400 font-bold">#{log.id}</td>
                    <td className="py-3 px-4 font-mono text-zinc-400">{log.createdAt?.replace('T', ' ').substring(0, 19)}</td>
                    <td className="py-3 px-4 font-mono font-bold text-white">{log.operatorId || 'ADMIN'}</td>
                    <td className="py-3 px-4 font-mono font-bold text-cyan-400">{log.userId ? `#${log.userId}` : '-'}</td>
                    <td className="py-3 px-4 font-bold uppercase text-amber-300">{log.action}</td>
                    <td className="py-3 px-4 font-mono text-zinc-400 max-w-xs truncate">
                      <span className="font-bold text-white mr-1.5">{log.httpMethod}</span>
                      {log.requestUri}
                    </td>
                    <td className="py-3 px-4 font-mono text-zinc-500">{log.clientIp || '127.0.0.1'}</td>
                    <td className="py-3 px-4 text-right">
                      <span className={`inline-flex px-2 py-0.5 rounded font-mono font-bold text-[10px] ${
                        log.httpStatus === 200 || log.httpStatus === 201 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'
                      }`}>
                        {log.httpStatus || 200}
                      </span>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8" className="py-8 text-center text-zinc-500">Không tìm thấy nhật ký kiểm toán nào.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
